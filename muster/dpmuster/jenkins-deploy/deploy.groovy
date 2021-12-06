// Please note that this script gets uploaded on every deploy from muster. Hi from muster!
// You can edit the script, but it's likely to be overwritten by muster at next deploy.
current_description = ""
slackThread = null
slackChannel = "proj-deployment"
slackUsername = "Jenkins"
gitUrl = 'ssh://git...'
vaultUrl = 'https://secrets.dataprofiler.com'
pipeline {
  agent {
    kubernetes {
      yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: jnlp
    image: 'jenkins/jnlp-slave:4.6-1'
    args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']  
    volumeMounts:
    - name: gitref
      mountPath: /var/dataprofiler-complete.git
  - name: builder
    image: container-registry.dataprofiler.com/jenkins-deploy:40
    workingDir: /home/jenkins-runner
    command:
    - cat
    tty: true
    envVars:
    - envVar:
        key: HELM_HOME
        value: /opt/helm
    volumeMounts:
    - name: dockersock
      mountPath: /var/run/docker.sock
    - name: docker
      mountPath: /usr/bin/docker
    - name: helm
      mountPath: /opt/helm/bin/helm
    - name: gitref
      mountPath: /var/dataprofiler-complete.git
  imagePullSecrets:
  - dataprofiler-registry-credentials
  volumes:
  - name: gitref
    hostPath: 
      path: /var/dataprofiler-complete.git
  - name: dockersock
    hostPath: 
      path: /var/run/docker.sock
  - name: docker
    hostPath: 
      path: /usr/bin/docker
  - name: helm
    hostPath: 
      path: /opt/helm/bin/helm
  - name: scratch
    emptyDir: {}
"""
    }
  }
  parameters {
    string(name: 'namespace', defaultValue: 'development', description: 'Kubernetes Namespace')
    string(name: 'revision', defaultValue: 'a641e105a421576029d5599f70f974361666caef', description: 'Git Revision')
    string(name: 'components', defaultValue: 'cluster', description: 'Components to deploy (cluster for all)')
    string(name: 'username', defaultValue: 'user', description: 'Username doing the deploy')
    string(name: 'nocache', defaultValue: 'false', description: 'Pass to build docker images without cache')
  }
  stages {
    stage('Notify Deploy Start') {
      steps {
        container('builder') {
          script {
            def blocks = [
              [
                "type": "section",
                "fields": [
                  [
                    "type": "mrkdwn",
                    "text": "*Kubernetes Cluster*\n${params.namespace}"
                  ],
                  [
                    "type": "mrkdwn",
                    "text": "*Components*\n${params.components}"
                  ],
                  [
                    "type": "mrkdwn",
                    "text": "*Revision*\n${params.revision.take(8)}"
                  ],
                  [
                    "type": "mrkdwn",
                    "text": "*User*\n${params.username}"
                  ]
                ]
              ],
              [
                "type": "actions",
                "elements": [
                  [
                    "type": "button",
                    "text": [
                      "type": "plain_text",
                      "text": "View Build Info"
                    ],
                    "style": "primary",
                    "url": "https://admin.dataprofiler.com/jenkins/blue/organizations/jenkins/deploy-${params.namespace}/detail/deploy-${params.namespace}/${env.BUILD_NUMBER}/pipeline/"
                  ]
                ]
              ]
            ]
            slackThread = slackSend(channel: slackChannel, username: slackUsername, blocks: blocks)
          }
        }
     }
    }
    stage('Checkout') {
      steps {
        container('jnlp') {
          checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: "${params.revision}"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CheckoutOption'], [$class: 'CloneOption', noTags: true, reference: '/var/dataprofiler-complete.git/']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'cluster-jenkins-git', url: gitUrl]]]
          script {
            current_description = "Revision: ${params.revision}\nDeployed:\n"
            currentBuild.description = current_description
          }
          slackSend(channel: slackThread.threadId,  username: slackUsername, message: "Checkout Complete")
        }
      }
    }
    stage('Prepare'){
      parallel {
        stage('Build/Spray Jars') {
          when {
           anyOf {
                expression { params.components == 'cluster' }
                expression { params.components == 'backend' }
                expression { params.components ==~ /.*api.*/ }
                expression { params.components ==~ /.*data-loading*/ }
            }
          }
          steps {
            container('builder') {
              sh './build.py --api-copy'
              sh "aws s3 sync web/api/data_profiler_core_jars s3://dataprofiler-dropbox/releases/${params.namespace}/"
              sh "aws s3 sync lib/iterators s3://dataprofiler-dropbox/releases/${params.namespace}-iterators/"
              slackSend(channel: slackThread.threadId,  username: slackUsername, message: "Build/Spray Jars Complete")
            }
          }
        }
        stage('Get/Set Secrets') {
          steps {
            container('builder') {
              withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh "create-configmap-from-vault ${params.namespace}"} // Find `create-configmap-from-vault` in dataprofiler-complete/docker/jenkins-deploy/create-configmap-from-vault.sh
              slackSend(channel: slackThread.threadId,  username: slackUsername, message: "Get/Set Secrets Complete")
            }
          }
        }
      }
    }
    stage("Deploy") {
      parallel {
        stage("Run Backend Ansible") {
          when {
           anyOf {
                expression { params.components == 'cluster' }
                expression { params.components == 'backend' }
                expression { params.components ==~ /.*api.*/ }
            }
          }
          steps {
            container('builder') {
              withCredentials([
                sshUserPrivateKey(credentialsId: 'converged-accumulo-cluster-ssh-agent', keyFileVariable: 'identity'),
                [$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]
                ]) { 
                sh "run-backend-ansible ${params.namespace}"
                script {
                  current_description = current_description + "\tBackend Ansible\n"
                  currentBuild.description = current_description
                }
                slackSend(channel: slackThread.threadId,  username: slackUsername, message: "Backend Ansible Complete")
              }
            }
          }
        }
        stage('API') {
          when {
           anyOf {
                expression { params.components == 'cluster' }
                expression { params.components ==~ /.*api.*/ }
            }
          }
          steps {
            container('builder') {
                withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' }
                sh "deploy-from-jenkins dp-api ${params.namespace} ${params.nocache} ${params.revision} web/api \$(get-kube-secret ${params.namespace} DNS_EXTERNAL_ADDRESS)"
                script {
                  current_description = current_description + "\tAPI\n"
                  currentBuild.description = current_description
                }
                slackSend(channel: slackThread.threadId,  username: slackUsername, message: "API Deployed")
                sh "python3 /usr/local/bin/run_integration_tests.py container-registry.dataprofiler.com/dp-api:${params.namespace}-${params.revision} dp-api ${params.namespace} --api_host=\$(get-kube-secret ${params.namespace} USER_FACING_API_HTTP_PATH) --api_pass=\$(get-kube-secret ${params.namespace} INTTESTPASS)"
            }
          }
        }
        stage('Unoconv') {
          when {
           anyOf {
                expression { params.components == 'cluster' }
                expression { params.components ==~ /.*api.*/ }
            }
          }
          steps {
            container('builder') {
                withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' }
                sh "deploy-from-jenkins dp-unoconv ${params.namespace} ${params.nocache} ${params.revision} doc-preview \$(get-kube-secret ${params.namespace} DNS_INTERNAL_ADDRESS)"
                script {
                  current_description = current_description + "\tUnoconv\n"
                  currentBuild.description = current_description
                }
                slackSend(channel: slackThread.threadId,  username: slackUsername, message: "Unoconv Deployed")
            }
          }
        }
        stage('UI') {
          when {
           anyOf {
                expression { params.components == 'cluster' }
                expression { params.components ==~ /.*ui.*/ } // TODO: this fires when you deploy ui-alt 
            }
          }
          steps {
            container('builder') {
                withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' }
                sh "deploy-from-jenkins dp-ui ${params.namespace} ${params.nocache} ${params.revision} dp-ui \$(get-kube-secret ${params.namespace} DNS_EXTERNAL_ADDRESS) --build-arg \$(read-env-var ${params.namespace} USER_FACING_API_HTTP_PATH) --build-arg USER_FACING_UI_HTTP_PATH=\$(get-kube-secret ${params.namespace} UI_MAIN_USER_FACING_HTTP_PATH) --build-arg \$(read-env-var ${params.namespace} MICROSOFT_AZURE_APP_ID)  --build-arg \$(read-env-var ${params.namespace} CLUSTER_NAME)  --build-arg \$(read-env-var ${params.namespace} SENTRY_UI_DSN) --build-arg \$(read-env-var ${params.namespace} SENTRY_UI_RELEASE_API_TOKEN) --build-arg \$(read-env-var ${params.namespace} ANALYTICS_UI_SITE_ID) --build-arg \$(read-env-var ${params.namespace} KIBANA_PARTIAL_URL)"
                script {
                  current_description = current_description + "\tUI\n"
                  currentBuild.description = current_description
                }
                slackSend(channel: slackThread.threadId,  username: slackUsername, message: "UI Deployed")
            }
          }
        }
        stage('Alternate UI') {
          when {
           anyOf {
                expression { params.components == 'cluster' }
                expression { params.components ==~ /.*ui-alt.*/ }
            }
          }
          steps {
            container('builder') {
                withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' }
                sh "deploy-from-jenkins dp-ui-alternate ${params.namespace} ${params.nocache} ${params.revision} web/ui \$(get-kube-secret ${params.namespace} DNS_EXTERNAL_ALT_ADDRESS) --build-arg \$(read-env-var ${params.namespace} USER_FACING_API_HTTP_PATH) --build-arg USER_FACING_UI_HTTP_PATH=\$(get-kube-secret ${params.namespace} UI_ALTERNATE_USER_FACING_HTTP_PATH) --build-arg \$(read-env-var ${params.namespace} MICROSOFT_AZURE_APP_ID)  --build-arg \$(read-env-var ${params.namespace} CLUSTER_NAME)  --build-arg \$(read-env-var ${params.namespace} SENTRY_UI_DSN) --build-arg \$(read-env-var ${params.namespace} SENTRY_UI_RELEASE_API_TOKEN) --build-arg \$(read-env-var ${params.namespace} ANALYTICS_UI_SITE_ID)  --build-arg USE_ALTERNATE_AUTH=true"
                script {
                  current_description = current_description + "\tAlt-UI\n"
                  currentBuild.description = current_description
                }
                slackSend(channel: slackThread.threadId,  username: slackUsername, message: "Alternate UI Deployed")
            }
          }
        }
        stage('ROU') {
          when {
           anyOf {
                expression { params.components == 'cluster' }
                expression { params.components ==~ /.*rules-of-use.*/ }
            }
          }
          steps {
             container('builder') {
                withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' }
                sh "deploy-from-jenkins dp-rou ${params.namespace} ${params.nocache} ${params.revision} rules-of-use-api \$(get-kube-secret ${params.namespace} DNS_INTERNAL_ADDRESS)"
                script {
                  current_description = current_description + "\tROU\n"
                  currentBuild.description = current_description
                }
                slackSend(channel: slackThread.threadId,  username: slackUsername, message: "ROU Deployed")
            }
          }
        }
        stage('Jobs API') {
          when {
              anyOf {
                  expression { params.components == 'cluster' }
                  expression { params.components ==~ /.*jobs.*/ }
              }
          }
          steps {
              container('builder') {
                  withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' }
                  sh "deploy-from-jenkins dp-jobs-api ${params.namespace} ${params.nocache} ${params.revision} jobs-api \$(get-kube-secret ${params.namespace} DNS_INTERNAL_ADDRESS)"
                  script {
                    current_description = current_description + "\tJobs API\n"
                    currentBuild.description = current_description
                  }
                  slackSend(channel: slackThread.threadId,  username: slackUsername, message: "Jobs API Deployed")
              }
          }
        }
        stage('Table Mapper API') {
          when {
              anyOf {
                  expression { params.components == 'cluster' }
                  expression { params.components ==~ /.*tables.*/ }
              }
          }
          steps {
              container('builder') {
                  withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' }
                  sh "deploy-from-jenkins dp-table-mapper-api ${params.namespace} ${params.nocache} ${params.revision} table-mapper-api \$(get-kube-secret ${params.namespace} DNS_INTERNAL_ADDRESS)"
                  script {
                    current_description = current_description + "\tTable Mapper API\n"
                    currentBuild.description = current_description
                  }
                  slackSend(channel: slackThread.threadId,  username: slackUsername, message: "Table Mapper API Deployed")
              }
          }
        }
        stage('Data Loading Daemon') {
          when {
            anyOf {
              expression { params.components == 'cluster' }
              expression { params.components ==~ /.*data-loading.*/ }
            }
          }
          steps {
            container('builder') {
                withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' }
                sh "deploy-from-jenkins data-loading-daemon ${params.namespace} ${params.nocache} ${params.revision} data-loading-daemon \$(get-kube-secret ${params.namespace} DNS_INTERNAL_ADDRESS)"
                script {
                  current_description = current_description + "\tData Loading Daemon\n"
                  currentBuild.description = current_description
                }
                slackSend(channel: slackThread.threadId,  username: slackUsername, message: "Data Loading Daemon Deployed")
            }
          }
        }
        // NOTE: python3 deploy_from_jenkins.py app namespace no_cache revision dockerfile_path  # args
        stage('Table Checker') {
          when {
           anyOf {
                expression { params.components == 'cluster' }
                expression { params.components ==~ /.*table-checker*/ }
            }
          }
          steps {
            container('builder') {
                withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' }
                sh "python3 /usr/local/bin/deploy_from_jenkins.py dp-table-checker ${params.namespace} ${params.nocache} ${params.revision} table-checker --docker_build_path .."
                script {
                  current_description = current_description + "\tTable Checker\n"
                  currentBuild.description = current_description
                }
                slackSend(channel: slackThread.threadId,  username: slackUsername, message: "Table Checker CronJob Deployed")  
            }
          }
        }
        stage('Tekton Job: Download') {
          when {
            anyOf {
              expression { params.components == 'cluster' }
              expression { params.components ==~ /.*tekton-download.*/ }
            }
          }
          steps {
            container('builder') {
              withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' } 
              sh "python3 /usr/local/bin/deploy_from_jenkins.py dp-download ${params.namespace} ${params.nocache} ${params.revision} download --docker_build_path .."
              script {
                current_description = current_description + "\tTekton Job: Download\n"
                currentBuild.description = current_description
              }
              slackSend(channel: slackThread.threadId, username: slackUsername, message: "Download Tekton Job/Pipeline Deployed")
            }
          }
        }
        stage('Tekton Job: SQLSync') {
          when {
            anyOf {
              expression { params.components == 'cluster' }
              expression { params.components ==~ /.*tekton-sqlsync.*/ }
            }
          }
          steps {
            container('builder') {
              withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' } 
              sh "python3 /usr/local/bin/deploy_from_jenkins.py dp-sqlsync ${params.namespace} ${params.nocache} ${params.revision} sqlsync --docker_build_path .."
              script {
                current_description = current_description + "\tTekton Job: SQLSync\n"
                currentBuild.description = current_description
              }
              slackSend(channel: slackThread.threadId, username: slackUsername, message: "SQLSync Tekton Job/Pipeline Deployed")
            }
          }
        }
        stage('Tekton Job: Dataset Performance') {
          when {
            anyOf {
              expression { params.components == 'cluster' }
              expression { params.components ==~ /.*tekton-ds-performance.*/ }
            }
          }
          steps {
            container('builder') {
              withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' } 
              sh "python3 /usr/local/bin/deploy_from_jenkins.py dp-dataset-performance ${params.namespace} ${params.nocache} ${params.revision} dataset-performance --docker_build_path .."
              script {
                current_description = current_description + "\tTekton Job: Dataset Performance\n"
                currentBuild.description = current_description
              }
              slackSend(channel: slackThread.threadId, username: slackUsername, message: "Dataset Performance Tekton Job/Pipeline Deployed")
            }
          }
        }
        stage('Tekton Job: Dataset Delta') {
          when {
            anyOf {
              expression { params.components == 'cluster' }
              expression { params.components ==~ /.*tekton-ds-delta.*/ }
            }
          }
          steps {
            container('builder') {
              withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' } 
              sh "python3 /usr/local/bin/deploy_from_jenkins.py dp-dataset-delta ${params.namespace} ${params.nocache} ${params.revision} dataset-delta --docker_build_path .."
              script {
                current_description = current_description + "\tTekton Job: Dataset Delta\n"
                currentBuild.description = current_description
              }
              slackSend(channel: slackThread.threadId, username: slackUsername, message: "Dataset Delta Tekton Job/Pipeline Deployed")
            }
          }
        }
        stage('Tekton Job: Dataset Quality') {
          when {
              anyOf {
                  expression { params.components == 'cluster' }
                  expression { params.components ==~ /.*tekton-ds-quality.*/ }
              }
          }
          steps {
              container('builder') {
                  withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' }
                  sh "python3 /usr/local/bin/deploy_from_jenkins.py dp-dataset-quality ${params.namespace} ${params.nocache} ${params.revision} dataset-quality --docker_build_path .."
                  script {
                      current_description = current_description + "\tTekton Job: Dataset Quality\n"
                      currentBuild.description = current_description
                  }
                  slackSend(channel: slackThread.threadId, username: slackUsername, message: "Dataset Quality Tekton Job/Pipeline Deployed")
              }
          }
        }
        stage('Tekton Job: Canceller') {
          when {
            anyOf {
              expression { params.components == 'cluster' }
              expression { params.components ==~ /.*tekton-canceller.*/ }
            }
          }
          steps {
            container('builder') {
              withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' } 
              sh "python3 /usr/local/bin/deploy_from_jenkins.py dp-tekton-job-canceller ${params.namespace} ${params.nocache} ${params.revision} tekton-canceller --docker_build_path .."
              script {
                current_description = current_description + "\tTekton Job: Canceller\n"
                currentBuild.description = current_description
              }
              slackSend(channel: slackThread.threadId, username: slackUsername, message: "Canceller Job/Pipeline Deployed")
            }
          }
        }
        stage('Tekton Job: Connection Engine') {
          when {
            anyOf {
              expression { params.components == 'cluster' }
              expression { params.components ==~ /.*tekton-con-engine.*/ }
            }
          }
          steps {
            container('builder') {
              withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' } 
              sh "python3 /usr/local/bin/deploy_from_jenkins.py dp-connection-engine ${params.namespace} ${params.nocache} ${params.revision} tekton-jobs/connection-engine --docker_build_path ../.."
              script {
                current_description = current_description + "\tTekton Job: Connection Engine\n"
                currentBuild.description = current_description
              }
              slackSend(channel: slackThread.threadId, username: slackUsername, message: "Connection Engine Job/Pipeline Deployed")
            }
          }
        }
        stage('LastMile RDS Monitor') {
          when {
              anyOf {
                  expression { params.components == 'cluster' }
                  expression { params.components ==~ /.*lastmile-monitor.*/ }
              }
          }
          steps {
              container('builder') {
                  withCredentials([[$class: 'VaultTokenCredentialBinding', credentialsId: 'jenkins-vault-approle', vaultAddr: vaultUrl]]) { sh 'get-kube-conf' }
                  sh "python3 /usr/local/bin/deploy_from_jenkins.py dp-lastmile-monitor ${params.namespace} ${params.nocache} ${params.revision} lastmile-monitor --docker_build_path .."
                  script {
                      current_description = current_description + "\tLastMile RDS Monitor"
                      currentBuild.description = current_description
                  }
                  slackSend(channel: slackThread.threadId, username: slackUsername, message: "LastMile RDS Monitor/Pipeline Deployed")
              }
          }
        }
      }
    }
    stage("Complete") {
      steps {
        container('builder') {
          sh 'exit 0' // Jenkins UI has this weird default to go to the last stage, even if it was skipped. Therefore, this is just a nice little convinience to always display something real in the UI.
        }
      }
    }
  }
  post { 
      success { 
        container('builder') {
          script {
            slackSend(channel: slackThread.threadId,  username: slackUsername, message: "Deploy Successful")
            slackThread.addReaction("white_check_mark")
          }
        }
      }
      unstable { 
        container('builder') {
          script {
            slackSend(channel: slackThread.threadId,  username: slackUsername, message: "Deploy Failure")
            slackThread.addReaction("red_circle")
          }
        }
      }
      failure { 
        container('builder') {
          script {
            slackSend(channel: slackThread.threadId,  username: slackUsername, message: "Deploy Failure")
            slackThread.addReaction("red_circle")
          }
        }
     }
     aborted {
        container('builder') {
          script {
            slackSend(channel: slackThread.threadId,  username: slackUsername, message: "Deploy Cancelled By User")
            slackThread.addReaction("red_circle")
          }
        }
     }
  }
}
