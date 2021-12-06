package main

import (
	"bufio"
	"crypto/tls"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"os/exec"
	"regexp"
	"strconv"
	"strings"
	"time"
)

// ENV Vars
var _zookeeper_env string = "ZOOKEEPERS"
var _rou_api_http_env string = "RULES_OF_USE_API_PATH"
var _api_http_env string = "USER_FACING_API_HTTP_PATH"
var _spark_sql_api_key_env string = "SPARK_SQL_API_KEY"
var _spark_sql_admin_username string = "SPARK_SQL_ADMIN_USERNAME"
var _spark_sql_admin_password string = "SPARK_SQL_ADMIN_PASSWORD"
var _spark_sql_api_username string = "SPARK_SQL_API_USERNAME"

// Global variables
var _namespace string = "default"
var _kube_admin_url = "https://kubernetes.default.svc"
var _serverHost string = ""
var _serverPort int = 7999
var _maxServerInstances int = 20
var _timeout int = 120
var _minServerInstanceStartPort = 11015
var _startCommand string = "/app/bin/spark-sql.sh"

// External shell scripts
var _createNodeportCmd string = "/app/sbin/create-nodeport.sh"
var _createPvCmd string = "/app/sbin/create-pv.sh"
var _createPvcCmd string = "/app/sbin/create-pvc.sh"
var _createTablesFromScriptCmd string = "/app/sbin/create-tables-from-script.sh"
var _createTablesCmd string = "/app/sbin/create-tables.sh"
var _dropTablesCmd string = "/app/sbin/drop-tables.sh"
var _showDatabasesCmd string = "/app/sbin/show-databases.sh"
var _showTablesCmd string = "/app/sbin/show-tables.sh"

// Global vars from ENV vars
var _rules_of_use_url string
var _api_datasets string
var _api_token string
var _admin_username string
var _admin_password string
var _api_user_name string

type StartInstnaceRequestBody struct {
	Username            string `json:"username"`
	Database            string `json:"database"`
	NumExecutors        int    `json:"numExecutors"`
	DriverMemory        string `json:"driverMemory"`
	DriverStorageSize   string `json:"driverStorageSize"`
	ExecutorMemory      string `json:"executorMemory"`
	ExecutorStorageSize string `json:"executorStorageSize"`
	LazyCache           bool   `json:"lazyCache"`
	LoadCatalog         bool   `json:"loadCatalog"`
	StorageClass        string `json:"storageClass"`
}

type DropDataRequestBody struct {
	Dataset string `json:"dataset"`
	Table   string `json:"table"`
}

type CreateTablesRequestBody struct {
	Dataset string `json:"dataset"`
	Table   string `json:"table"`
}

type ShowTablesRequestBody struct {
	Dataset string `json:"dataset"`
}

type ShowTablesResponse struct {
	Database    string `json:"database"`
	TableName   string `json:"tableName"`
	IsTemporary bool   `json:"isTemporary"`
}

type StartInstanceResponse struct {
	Auths    []string `json:"auths"`
	Name     string   `json:"name"`
	Pid      int      `json:"pid"`
	Url      string   `json:"url"`
	Uid      string   `json:"uid"`
	Username string   `json:"username"`
	Status   string   `json:"status"`
}

type StopInstanceresponse struct {
	Message string `json:"message"`
	Status  string `json:"status"`
}

type InstanceStatusResponse struct {
	Name             string `json:"name"`
	Status           string `json:"status"`
	Uid              string `json:"uid"`
	Url              string `json:"url"`
	Username         string `json:"username"`
	SparkAppSelector string `json:"sparkAppSelector"`
}

type ErrorMessage struct {
	Message string `json:"message"`
	Status  string `json:"status"`
}

type LoadeDataResponse struct {
	Dataset string   `json:"dataset"`
	Tables  []string `json:"tables"`
}

func handleRequests() {
	http.HandleFunc("/status", status)
	http.HandleFunc("/create-tables/", createTables)
	http.HandleFunc("/drop-tables/", dropTables)
	http.HandleFunc("/show-tables/", showTables)
	http.HandleFunc("/load-catalog/", loadCatalog)
	http.HandleFunc("/show-databases/", showDatabases)
	http.HandleFunc("/start-instance", startInstance)
	http.HandleFunc("/stop-instance/", stopInstance)

	log.Fatal(http.ListenAndServe(fmt.Sprintf(":%d", _serverPort), nil))
}

// Name:        status
// Description: Return the server status
// Inputs:      w http.ResponseWriter
//              r *http.Request
// Outputs:
func status(w http.ResponseWriter, r *http.Request) {
	log.Printf(fmt.Sprintf("Response from host: %s", r.Host))

	instances := loadServerInstances()

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(instances)
}

// Name:        checkIfServerInstanceExists
// Description: Determine if an instance is running for a current user
// Inputs:      Username String
// Outputs:     bool - true if exists; false otherwise
func checkIfServerInstanceExists(username string) bool {
	instances := loadServerInstances()
	for _, inst := range instances {
		if username == inst.Username {
			return true
		}
	}
	return false
}

// Name:        startInstance
// Description: Start a server instance command
// Inputs:      w http.ResponseWriter
//              r *http.Request
// Outputs:
func startInstance(w http.ResponseWriter, r *http.Request) {

	requestBody, _ := ioutil.ReadAll(r.Body)

	// Defaults config values
	jobConfig := StartInstnaceRequestBody{
		NumExecutors:        1,
		DriverMemory:        "2g",
		DriverStorageSize:   "2G",
		ExecutorMemory:      "2g",
		ExecutorStorageSize: "2G",
		LazyCache:           false,
		LoadCatalog:         true,
		StorageClass:        "efs-sc"}

	json.Unmarshal(requestBody, &jobConfig)

	// Set config values
	driverMem := jobConfig.DriverMemory
	driverStorageSize := jobConfig.DriverStorageSize
	executorMem := jobConfig.ExecutorMemory
	executorStorageSize := jobConfig.ExecutorStorageSize
	numExecutors := jobConfig.NumExecutors
	lazyCache := jobConfig.LazyCache
	loadCatalog := jobConfig.LoadCatalog
	username := jobConfig.Username

	// Check if the user already has an instance
	if checkIfServerInstanceExists(username) {
		rsp := ErrorMessage{
			Message: fmt.Sprintf("spark-sql-%s is already running", username),
			Status:  "Failed"}
		w.WriteHeader(http.StatusInternalServerError)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(rsp)
		return
	}

	// Get user's authorizations
	userAuths, err := getUserAttributes(username)
	if err != nil {
		log.Print("ERROR: ", err)
		rsp := ErrorMessage{Message: err.Error(), Status: "Failed"}
		w.WriteHeader(http.StatusInternalServerError)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(rsp)
		return
	}

	log.Printf(fmt.Sprintf("User '%s' has attributes %s", username, strings.Join(userAuths, ",")))
	if len(userAuths) == 0 {
		rsp := ErrorMessage{
			Message: fmt.Sprintf("User '%s' does not possess any authorizations", username),
			Status:  "Failed"}
		w.WriteHeader(http.StatusUnauthorized)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(rsp)
		return
	}

	// Get zookeepers from env var
	zookeepers, ok := os.LookupEnv(_zookeeper_env)
	if !ok {
		rsp := ErrorMessage{
			Message: fmt.Sprintf("Zookeeper environment var not found"),
			Status:  "Failed"}
		w.WriteHeader(http.StatusUnauthorized)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(rsp)
		return
	}

	applicationName := fmt.Sprintf("spark-sql-%s", username)

	// Setup volumes for driver and executors
	err = createVolume(username, applicationName, driverStorageSize, "driver")
	if err != nil {
		log.Print("ERROR: ", err)
		rsp := ErrorMessage{Message: err.Error(), Status: "Failed"}
		w.WriteHeader(http.StatusInternalServerError)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(rsp)
		return
	}

	err = createVolume(username, applicationName, executorStorageSize, "executor")
	if err != nil {
		log.Print("ERROR: ", err)
		rsp := ErrorMessage{Message: err.Error(), Status: "Failed"}
		w.WriteHeader(http.StatusInternalServerError)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(rsp)
		return
	}

	// Start server instanceout
	cmd := exec.Command(_startCommand, _namespace, username, driverMem, executorMem, fmt.Sprintf("%d", numExecutors), zookeepers)
	cmd.Stdout = os.Stdout
	err = cmd.Start()
	if err != nil {
		log.Print("ERROR: ", err)
		rsp := ErrorMessage{Message: err.Error(), Status: "Failed"}
		w.WriteHeader(http.StatusInternalServerError)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(rsp)
		return
	}

	// Wait for server to start
	ssr := loadUid(applicationName)
	deadline := time.Now().Add(time.Duration(_timeout) * time.Second)
	for {
		log.Print("Waiting for instance to report as 'Running'....")
		time.Sleep(5 * time.Second)
		ssr = loadUid(applicationName)
		log.Print("ServerStatusResponse:\n", ssr)
		if ssr.Status == "Pending" || ssr.Status == "Running" || time.Now().After(deadline) {
			break
		}
	}

	if ssr.Status != "Pending" && ssr.Status != "Running" {
		log.Print("Failed to start server instance due to timeout")
		rsp := ErrorMessage{Message: fmt.Sprintf("Failed to start server instance: %s", ssr), Status: "Failed"}
		w.WriteHeader(http.StatusInternalServerError)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(rsp)
		return
	}

	// Create NodePort if one doesn't exist already
	instancePort := getNodePort(applicationName)
	if instancePort != -1 {
		log.Printf("Current NodePort for %s is %d", applicationName, instancePort)
	} else {
		// Create Nodeport Service
		fmt.Println("Creating Nodeport Service:", _namespace, applicationName)
		out, err := createNodeportServiceCmd(applicationName)
		if err != nil {
			log.Print("ERROR: ", err)
			rsp := ErrorMessage{Message: err.Error(), Status: "Failed"}
			w.WriteHeader(http.StatusInternalServerError)
			w.Header().Set("Content-Type", "application/json")
			json.NewEncoder(w).Encode(rsp)
			return

		}
		log.Printf("NodePort Service: %s\n", string(out))
		instancePort = getNodePort(applicationName)
	}

	// Construct the JDBC connection String
	instanceUrl := fmt.Sprintf("jdbc:hive2://%s:%d", _serverHost, instancePort)
	fmt.Println("JDBC connection string: ", instanceUrl)

	// Construct server start response
	rsp := StartInstanceResponse{
		Auths:    userAuths,
		Uid:      ssr.Uid,
		Name:     ssr.Name,
		Status:   ssr.Status,
		Url:      instanceUrl,
		Username: username,
		Pid:      cmd.Process.Pid}

	// Load catalog
	if loadCatalog {
		log.Printf(fmt.Sprintf("Loading Catalog for application: %s", applicationName))
		go loadCatalogTables(applicationName, username, lazyCache)
	}

	w.WriteHeader(http.StatusOK)
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(rsp)
}

// Name:        loadCatalog
// Description: Loads all datasets and tables for a user
// Inputs:      w http.ResponseWriter
//              r *http.Request
// Outputs:
func loadCatalog(w http.ResponseWriter, r *http.Request) {

	// Determine instance UID
	parts := strings.Split(r.URL.Path, "/")
	uid := parts[2]

	// Get instance by UID
	instance, err := getInstanceByUid(uid)

	if err != nil {
		rsp := StopInstanceresponse{
			Message: err.Error(),
			Status:  "Error"}

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(rsp)
		return
	}

	// Load the catalog
	loadedData, err := loadCatalogTables(instance.Name, instance.Username, false)
	if err != nil {
		rsp := StopInstanceresponse{
			Message: err.Error(),
			Status:  "Error"}

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(rsp)
		return
	}

	w.WriteHeader(http.StatusOK)
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(loadedData)
}

// Name:        loadCatalogTables
// Description: Load all of tables for an application
// Inputs:      applicationName string - The name of the application
//              username string - The name of the users
//				enableLazyCache bool - Enable cache
// Outputs:		loadedData []LoadeDataResponse - The data that was loaded
func loadCatalogTables(applicationName string, username string, enableLazyCache bool) (loadedData []LoadeDataResponse, err error) {

	// Get attributes for the specified user
	userAttributes, err := getUserAttributes(username)
	if err != nil {
		log.Print("Error: ", err)
		return loadedData, err
	}

	// Get all active visibilities
	activeAttributes, err := getAllActiveVisibilities()
	if err != nil {
		log.Print("Error: ", err)
		return loadedData, err
	}

	// Compute visibilities to restrict
	attributesToReject := difference(activeAttributes, userAttributes)

	// Update the API user visibilities
	// This is kind of a hack. It will assign the API user all available visibilities, ensuring
	// the following API calls can complete.
	_, err = setApiUserAttributes(activeAttributes)
	if err != nil {
		log.Print("Error: ", err)
		return loadedData, err
	}

	// Get the datasets for the user
	datasets, err := getDatasets(attributesToReject)
	if err != nil {
		log.Print("Error: ", err)
		return loadedData, err
	}

	log.Println("Found Datasets: " + strings.Join(datasets, ","))

	// Create a temp file
	tmpFile, err := ioutil.TempFile(os.TempDir(), username+"-")
	if err != nil {
		log.Fatal("Cannot create temporary file", err)
	}

	// Clean up the temp file after we are done with it
	defer os.Remove(tmpFile.Name())

	log.Println("Created temp file: " + tmpFile.Name())

	// Convert the user auths from an array to a comma separated string
	userAuths := strings.Join(userAttributes, ",")

	// Iterate over the datasets to get the tables
	for _, dataset := range datasets {
		tables, err := getTables(dataset, attributesToReject)

		log.Println("Found Tables: " + strings.Join(tables, ","))
		if err != nil {
			log.Print("Error: ", err)
			return loadedData, err
		}

		if len(tables) > 0 {

			data := LoadeDataResponse{
				Dataset: dataset,
				Tables:  tables,
			}

			loadedData = append(loadedData, data)

			tmpFile.WriteString(createSqlDatabaseCommand(dataset))
			tmpFile.WriteString("\n\n")
		}
		for _, table := range tables {
			tmpFile.WriteString(dropSqlTableCommand(dataset, table))
			tmpFile.WriteString("\n\n")
			tmpFile.WriteString(createSqlLoadCommand(dataset, table, userAuths, false))
			tmpFile.WriteString("\n\n")
		}
	}

	if err := tmpFile.Close(); err != nil {
		log.Fatal("Could not close the temp file", err)
	}

	cmd := exec.Command(_createTablesFromScriptCmd, applicationName, tmpFile.Name(), _admin_username, _admin_password)

	err = cmd.Run()
	if err != nil {
		fmt.Print("Error: ", err.Error())
		return loadedData, err
	}
	log.Print(fmt.Sprintf("Successfully loaded tables for user: %s", username))

	return loadedData, nil
}

func difference(a, b []string) (diff []string) {
	m := make(map[string]bool)

	for _, item := range b {
		m[item] = true
	}

	for _, item := range a {
		if _, ok := m[item]; !ok {
			diff = append(diff, item)
		}
	}
	return
}

// Name:        createSqlDatabaseCommand
// Description:	Construct a create database statement with the provided dataset
// Inputs:		dataset string - The name of the database to create
// Outputs:     string - The create statement
func createSqlDatabaseCommand(dataset string) string {
	databaseName := normalizeName(dataset)
	query := fmt.Sprintf("CREATE DATABASE IF NOT EXISTS %s;", databaseName)
	return query
}

// Name:        dropSqlTableCommand
// Description:	Construct a drop table statement with the provided dataset and table
// Inputs:		dataset string - The name of the dataset
//				table string - The name of the table to drop
// Outputs:     string - The drop statement
func dropSqlTableCommand(dataset string, table string) string {
	databaseName := normalizeName(dataset)
	tableName := normalizeName(table)
	query := fmt.Sprintf("DROP TABLE IF EXISTS %s.%s;", databaseName, tableName)
	return query
}

// Name:        createSqlLoadCommand
// Description:	Construct a create table statement with the provided details
// Inputs:		dataset string - The name of the dataset
//				table string - The name of the table
//				auths string - The authorizations to apply
//				enableLazyCache bool - Cache the table?
// Outputs:		string - The create table statement
func createSqlLoadCommand(dataset string, table string, auths string, enableLazyCache bool) string {
	databaseName := normalizeName(dataset)
	tableName := normalizeName(table)
	query := fmt.Sprintf("CREATE TABLE %s.%s USING org.apache.spark.sql.sources.accumulo.v1 "+
		"OPTIONS (dataScanSpec=\"{\\\"dataset\\\":\\\"%s\\\",\\\"table\\\":\\\"%s\\\"}\",auths=\"%s\");",
		databaseName, tableName, dataset, table, auths)
	if enableLazyCache {
		query = query + fmt.Sprintf("CACHE LAZY TABLE %s.%s;\n", databaseName, tableName)
	}
	return query
}

// Name:        normalizeName
// Description:	Remove all of the non-word charcaters from a name and replace with underscore
// Inputs:		s string - The name to normalize
// Outputs:		string - The normalized name
func normalizeName(s string) string {
	sampleRegexp := regexp.MustCompile(`\W+`)
	result := sampleRegexp.ReplaceAllString(s, "_")
	return result
}

// Name:        createVolume
// Description: Create PV and PVC
// Inputs:      usesrname string
//              applicationName string
//              storageSize string
//              sparkRole string
// Outputs:
func createVolume(username string, applicationName string, storageSize string, sparkRole string) error {

	// Create Persistent Volume
	volumeName := fmt.Sprintf("spark-sql-%s-%s-%s", username, _namespace, sparkRole)
	_, err := createPersistentVolume(volumeName, "efs-sc", storageSize, applicationName, sparkRole)
	if err != nil {
		log.Printf(err.Error())
		return err
	}

	// Create Persistent Volume Claim
	claimName := fmt.Sprintf("spark-sql-%s-%s-%s", username, _namespace, sparkRole)
	_, err = createPersistentVolumeClaim(claimName, "efs-sc", storageSize, applicationName, sparkRole)
	if err != nil {
		log.Printf(err.Error())
		return err
	}
	return nil
}

// Name:        createPersistentVolume
// Description: Create PV
// Inputs:      name string - The name of the PV
//              storageClass string - The storage class of the PV
//              size string - The size of the volume
//              podName string - The name of the pod to attach the PV to
//              role string - The name of the role
// Outputs:     out []byte
//              err error
func createPersistentVolume(name string, storageClass string, size string, podName string, role string) (out []byte, err error) {
	log.Printf("Creating Persistent Volume [%s %s %s %s %s %s]", _namespace, name, storageClass, size, podName, role)
	log.Printf("Running Command: %s %s %s %s %s %s %s", _createPvCmd, _namespace, name, storageClass, size, podName, role)

	cmd := exec.Command(_createPvCmd, _namespace, name, storageClass, size, podName, role)
	return cmd.Output()
}

// Name:        createPersistentVolumeClaim
// Description: Create PVC
// Inputs:      name string - The name of the PVC
//              storageClass string - The storage class of the PVC
//              size string - The size of the volume
//              podName string - The name of the pod to attach the PVC to
//              role string - The name of the role
// Outputs:     out []byte
//              err error
func createPersistentVolumeClaim(name string, storageClass string, size string, podName string, role string) (out []byte, err error) {
	log.Printf("Creating Persistent Volume Claim [%s %s %s %s %s %s]", _namespace, name, storageClass, size, podName, role)
	cmd := exec.Command(_createPvcCmd, _namespace, name, storageClass, size, podName, role)
	return cmd.Output()
}

// Name:        deleteVolume
// Description: Delete PVC and PV
// Inputs:      applicationName string
//              sparkRole string
// Outputs:
func deleteVolume(applicationName string, sparkRole string) {

	// Delete PVC
	log.Print(fmt.Sprintf("Removing PVC for application %s", applicationName))
	claimName := fmt.Sprintf("%s-%s-%s", applicationName, _namespace, sparkRole)
	deleteCmd("persistentvolumeclaims", claimName)

	// Delete PV
	log.Print(fmt.Sprintf("Removing PV for application %s", applicationName))
	volumeName := fmt.Sprintf("%s-%s-%s", applicationName, _namespace, sparkRole)
	deleteClusterScopedCmd("persistentvolumes", volumeName)
}

// Name:        stopInstance
// Description: Stop the instance and remove any dependencies
// Inputs:      w http.ResponseWriter
//              r *http.Request
// Outputs:
func stopInstance(w http.ResponseWriter, r *http.Request) {
	log.Print("stopInstance()", r.URL.Path)
	parts := strings.Split(r.URL.Path, "/")
	uid := parts[2]
	log.Printf("Stopping Instance %s\n", uid)

	// Get the instance by UID
	instance, err := getInstanceByUid(uid)

	if err != nil {
		rsp := StopInstanceresponse{
			Message: err.Error(),
			Status:  "Error"}

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(rsp)
		return
	}

	applicationName := instance.Name
	log.Print(fmt.Sprintf("Removing Pods for application %s", applicationName))

	// remote the pods associated with this instance
	deleteCmd("pods", applicationName)

	// Remove the volumes associated with this instance
	deleteVolume(applicationName, "driver")
	deleteVolume(applicationName, "executor")

	rsp := StopInstanceresponse{
		Message: fmt.Sprintf("Stopped instance %s", applicationName),
		Status:  "Success"}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(rsp)
}

// Name:        createTables
// Description: Create the specified tables
// Inputs:      w http.ResponseWriter
//              r *http.Request
// Outputs:
func createTables(w http.ResponseWriter, r *http.Request) {

	// Determine instance UID
	parts := strings.Split(r.URL.Path, "/")
	uid := parts[2]

	requestBody, _ := ioutil.ReadAll(r.Body)
	var dataLoads []CreateTablesRequestBody
	json.Unmarshal(requestBody, &dataLoads)

	// Get the instance by UID
	instance, err := getInstanceByUid(uid)

	if err != nil {
		rsp := StopInstanceresponse{
			Message: err.Error(),
			Status:  "Error"}

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(rsp)
		return
	}

	applicationName := instance.Name

	// Get user's authorizations
	userAuths, nil := getUserAttributes(instance.Username)
	if err != nil {
		rsp := StopInstanceresponse{
			Message: err.Error(),
			Status:  "Error"}

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(rsp)
		return
	}

	userAuthsString := strings.Join(userAuths, ",")

	log.Printf(fmt.Sprintf("User '%s' has attributes %s", instance.Username, userAuthsString))
	if len(userAuths) == 0 {
		rsp := ErrorMessage{
			Message: fmt.Sprintf("User '%s' does not possess any authorizations", instance.Username),
			Status:  "Failed"}
		w.WriteHeader(http.StatusUnauthorized)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(rsp)
		return
	}

	// Create the tables
	log.Printf("Applying %s", instance)
	for _, load := range dataLoads {
		log.Print(fmt.Sprintf("Calling funct with opts: %s %s %s %s", applicationName, load.Dataset, load.Table, userAuthsString))
		cmd := exec.Command(_createTablesCmd, applicationName, load.Dataset, load.Table, userAuthsString, _admin_username, _admin_password)
		_, err := cmd.Output()
		if err != nil {
			log.Print("ERROR:", err.Error())
			w.WriteHeader(http.StatusInternalServerError)
			return
		}
	}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
}

// Name:        dropTables
// Description: Remove the specified tables
// Inputs:      w http.ResponseWriter
//              r *http.Request
// Outputs:
func dropTables(w http.ResponseWriter, r *http.Request) {

	// Determine instance UID
	parts := strings.Split(r.URL.Path, "/")
	uid := parts[2]

	// Get the instance by UID
	instance, err := getInstanceByUid(uid)

	if err != nil {
		rsp := StopInstanceresponse{
			Message: err.Error(),
			Status:  "Error"}

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(rsp)
		return
	}

	applicationName := instance.Name

	requestBody, _ := ioutil.ReadAll(r.Body)
	var tablesToDrop []DropDataRequestBody
	json.Unmarshal(requestBody, &tablesToDrop)

	for _, tableToDrop := range tablesToDrop {
		log.Print(fmt.Sprintf("Running command %s %s %s %s", _dropTablesCmd, applicationName, tableToDrop.Dataset, tableToDrop.Table))
		cmd := exec.Command(_dropTablesCmd, applicationName, tableToDrop.Dataset, tableToDrop.Table, _admin_username, _admin_password)
		_, err := cmd.Output()
		if err != nil {
			log.Print("ERROR: ", err)
			w.WriteHeader(http.StatusInternalServerError)
			return
		}
	}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
}

// Name:        showDatabases
// Description: Show available databases
// Inputs:      w http.ResponseWriter
//              r *http.Request
// Outputs:
func showDatabases(w http.ResponseWriter, r *http.Request) {

	// Determine the instance UID
	parts := strings.Split(r.URL.Path, "/")
	uid := parts[2]

	instance, err := getInstanceByUid(uid)

	if err != nil {
		rsp := StopInstanceresponse{
			Message: err.Error(),
			Status:  "Error"}

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(rsp)
		return
	}

	applicationName := instance.Name

	log.Print("Application name:" + applicationName)
	log.Print(fmt.Sprintf("Calling func with opts: %s", applicationName))

	cmd := exec.Command(_showDatabasesCmd, applicationName, _admin_username, _admin_password)
	out, err := cmd.Output()
	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		errMsgJson := fmt.Sprintf("{\"error\":\"%s\"}", err.Error())
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(errMsgJson)
		return
	}
	data, err := parseDatabaseOutputJson(string(out))
	if err != nil {
		w.WriteHeader(http.StatusConflict)
	}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(data)
}

// Name:        showTables
// Description: Show all available tables
// Inputs:      w http.ResponseWriter
//              r *http.Request
// Outputs:
func showTables(w http.ResponseWriter, r *http.Request) {

	// Determine the instnace UID
	parts := strings.Split(r.URL.Path, "/")
	uid := parts[2]

	// Read the request body
	requestBody, _ := ioutil.ReadAll(r.Body)
	var showTables ShowTablesRequestBody
	json.Unmarshal(requestBody, &showTables)

	// Get the instance by UID
	instance, err := getInstanceByUid(uid)

	if err != nil {
		rsp := StopInstanceresponse{
			Message: err.Error(),
			Status:  "Error"}

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(rsp)
		return
	}

	applicationName := instance.Name

	log.Print("Application name:" + applicationName)
	log.Print(fmt.Sprintf("Calling func with opts: %s %s", applicationName, showTables.Dataset))

	cmd := exec.Command(_showTablesCmd, applicationName, showTables.Dataset, _admin_username, _admin_password)
	out, err := cmd.Output()
	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		errMsgJson := fmt.Sprintf("{\"error\":\"%s\"}", err.Error())
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(errMsgJson)
		return
	}

	data, err := parseTableViewOutputJson(string(out))
	if err != nil {
		w.WriteHeader(http.StatusConflict)
	}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(data)
}

// Name:        createNodeportServiceCmd
// Description: Create Nodeport Service
// Inputs:      name string - The name of the nodeport
// Outputs:     out []byte
//              err error
func createNodeportServiceCmd(name string) (out []byte, err error) {
	cmd := exec.Command(_createNodeportCmd, _namespace, name)
	return cmd.Output()
}

// Name:        parseTableViewOutputJson
// Description: Parse tables returned by beeline 'show tables' command
// +-----------+--------------+--------------+--+
// | database  |  tableName   | isTemporary  |
// +-----------+--------------+--------------+--+
// | default   | cell_public  | false        |
// | default   | mmd_public   | false        |
// | default   | test_public  | false        |
// +-----------+--------------+--------------+--+
// Inputs:      data string - The output of the 'show tables' command
// Outputs:     table []Table
//              err error
func parseTableViewOutputJson(data string) (tables []ShowTablesResponse, err error) {

	scanner := bufio.NewScanner(strings.NewReader(data))
	var lines []string
	for scanner.Scan() {
		lines = append(lines, scanner.Text())
	}

	err = scanner.Err()
	for _, line := range lines {
		if strings.HasPrefix(line, "|") {
			parts := strings.Split(line, "|")

			if len(parts) == 5 {
				db := strings.TrimSpace(parts[1])
				tbl := strings.TrimSpace(parts[2])
				tmp := strings.TrimSpace(parts[3])
				isTemp, _ := strconv.ParseBool(tmp)
				table := ShowTablesResponse{Database: db, TableName: tbl, IsTemporary: isTemp}
				tables = append(tables, table)
			}
		}
	}

	// Omit the first element as it is the header
	return tables[1:], err
}

// Name:        parseTableViewOutputJson
// Description: Parse databases returned by beeline 'show databases' command
// +---------------+--+
// | databaseName  |
// +---------------+--+
// | default       |
// | test          |
// +---------------+--+
// Inputs:      data string - The output of the 'show databases' command
// Outputs:     database []string
//              err error
func parseDatabaseOutputJson(data string) (databases []string, err error) {

	scanner := bufio.NewScanner(strings.NewReader(data))
	var lines []string
	for scanner.Scan() {
		lines = append(lines, scanner.Text())
	}
	err = scanner.Err()
	for _, line := range lines {
		if strings.HasPrefix(line, "|") {

			parts := strings.Split(line, "|")
			if len(parts) == 3 {
				db := strings.TrimSpace(parts[1])
				databases = append(databases, db)
			}
		}
	}

	// Omit the first element as it is the header
	return databases[1:], err
}

// Name:        loadServerInstances
// Description: List existing server instances
// Inputs:
// Outputs:     map[string]ServerStatusResponse
func loadServerInstances() map[string]InstanceStatusResponse {

	instances := make(map[string]InstanceStatusResponse)
	body, err := listPodsCmd("app=spark-sql")
	if err != nil {
		fmt.Println(err)
		return instances
	}
	var rsp struct {
		Items []struct {
			Metadata struct {
				Uid    string
				Name   string
				Labels struct {
					SparkAppSelector string `json:"spark-app-selector"`
				}
			}
			Status struct{ Phase string }
		}
	}
	json.Unmarshal(body, &rsp)

	// Build the response
	for _, item := range rsp.Items {
		uid := item.Metadata.Uid
		name := item.Metadata.Name
		status := item.Status.Phase
		selector := item.Metadata.Labels.SparkAppSelector
		instanceUrl := fmt.Sprintf("jdbc:hive2://%s:%d", _serverHost, getNodePort(item.Metadata.Name))
		username := getUsernameFromPodName(name)

		ssr := InstanceStatusResponse{
			Name:             name,
			Status:           status,
			Uid:              uid,
			Url:              instanceUrl,
			Username:         username,
			SparkAppSelector: selector}
		instances[uid] = ssr
	}
	return instances
}

// Name:        getNodePort
// Description: List existing server instances
// Inputs:      name string - The name of the service
// Outputs:     map[string]ServerStatusResponse
func getNodePort(name string) int {

	body, err := listServicesCmd(name)
	if err != nil {
		log.Print("ERROR: ", err.Error())
	}
	log.Print("NODEPORT SERVICE:", string(body))
	var rsp struct {
		Spec struct {
			Ports []struct {
				NodePort int
			}
		}
	}
	err = json.Unmarshal(body, &rsp)
	if err != nil {
		return -1
	}
	if len(rsp.Spec.Ports) > 0 {
		return rsp.Spec.Ports[0].NodePort
	}
	return -1
}

// Name:        loadUid
// Description:
// Inputs:      name string - The name of the pod
// Outputs:     ServerStatusResponse
func loadUid(name string) InstanceStatusResponse {
	body, err := listPodCmd(name)
	if err != nil {
		log.Print("Error occurred:", err)
		return InstanceStatusResponse{}
	}
	var rsp struct {
		Metadata struct {
			Uid    string
			Name   string
			Labels struct {
				SparkAppSelector string `json:"spark-app-selector"`
			}
		}
		Status struct{ Phase string }
	}
	err = json.Unmarshal(body, &rsp)
	if err != nil {
		log.Print("don't panic....", err)
	}
	uid := rsp.Metadata.Uid
	podName := rsp.Metadata.Name
	status := rsp.Status.Phase
	username := getUsernameFromPodName(podName)
	appSelector := rsp.Metadata.Labels.SparkAppSelector
	ssr := InstanceStatusResponse{
		Name:             podName,
		Uid:              uid,
		Status:           status,
		Username:         username,
		SparkAppSelector: appSelector}
	return ssr
}

// Name:        getDatasets
// Description: Get the list of datasets available for a user
// Inputs:      attributesToReject []string - The attributes the user does not have
// Outputs:		[] string - The list of datasets
//				error
func getDatasets(attributesToReject []string) ([]string, error) {
	log.Print("Retrieving Datasets")

	var datasets []string
	body, err := runApiCurlCmd("/v1/datasets", attributesToReject)
	if err != nil {
		log.Print("Error:", err)
		return datasets, errors.New(err.Error())
	}
	var result map[string]map[string]interface{}
	json.Unmarshal([]byte(body), &result)

	for dataset := range result {
		datasets = append(datasets, dataset)
	}

	return datasets, nil
}

// Name:        getTables
// Description: Get the list of tables available for a user
// Inputs:      dataset string - The dataset to search for
//				attributesToReject []string - The attributes the user does not have
// Outputs:		[] string - The list of tables
//				error
func getTables(dataset string, attributesToReject []string) ([]string, error) {
	log.Printf("Retrieving tables for Dataset '%s'", dataset)

	path := fmt.Sprintf("/v1/tables/%s", dataset)

	var tables []string
	body, err := runApiCurlCmd(path, attributesToReject)
	if err != nil {
		log.Print("Error:", err)
		return tables, errors.New(err.Error())
	}
	var result map[string]interface{}
	json.Unmarshal([]byte(body), &result)

	for table := range result {
		tables = append(tables, table)
	}

	return tables, nil
}

// Name:        getInstanceByUid
// Description: Get an instance given its UID
// Inputs:      uid string - The instance UID
// Outputs:		instance InstanceStatusResponse - The instance
//				error
func getInstanceByUid(uid string) (instance InstanceStatusResponse, err error) {
	// Get the list of instances
	instances := loadServerInstances()

	if len(instances) == 0 {
		return instance, errors.New("No instances could be found")
	}

	// Get the specified instance
	instance, ok := instances[uid]

	if !ok {
		return instance, errors.New(fmt.Sprintf("Instance with id '%s' was not found", uid))
	}
	return instance, nil
}

// Name:        getUserAttributes
// Description: Get the list attributes for a user
// Inputs:      username string - The name of the user
// Outputs:     userVisibilities []string - The list of visibilities
func getUserAttributes(username string) (userVisibilities []string, err error) {
	log.Printf("Retrieving visibilities for user '%s'", username)

	reqBody := "{\"query\":\"{user(username:\\\"" + username + "\\\"){username attributes {id is_active value }}}\"}"

	body, err := runRouCurlCmd(reqBody)
	if err != nil {
		log.Print("Error: ", err)
		return userVisibilities, err
	}
	var rsp struct {
		Data struct {
			User struct {
				Username   string
				Attributes []struct {
					Value string `json:"value"`
				}
			}
		}
	}
	err = json.Unmarshal(body, &rsp)
	if err != nil {
		log.Print("Error: ", err)
		return userVisibilities, err
	}

	for _, item := range rsp.Data.User.Attributes {
		if strings.HasPrefix(item.Value, "LIST") {
			userVisibilities = append(userVisibilities, item.Value)
		}
	}
	return userVisibilities, nil
}

// Name:        setApiUserAttributes
// Description: Set the visibilities of the api user
// Inputs:      activeVisibilities []string - A list of all active visibilities
// Outputs:     userVisibilities []string - The list of the users visibilities
func setApiUserAttributes(activeVisibilities []string) (userVisibilities []string, err error) {
	log.Printf("Setting visibilities for user '%s'", _api_user_name)

	activeVisibilitiesString := fmt.Sprintf("[\"%s\"]", strings.Join(activeVisibilities, "\",\""))

	reqBody := fmt.Sprintf("{\"query\":\"mutation ($username: String, $attributes: [String]) {createUpdateUser(username: $username, attributes: $attributes) {id username first_name last_name position attributes {id is_active value}}}\",\"variables\":{\"username\":\"%s\",\"attributes\":%s}}", _api_user_name, activeVisibilitiesString)

	body, err := runRouCurlCmd(reqBody)
	if err != nil {
		log.Print("Error: ", err)
		return userVisibilities, err
	}
	var rsp struct {
		Data struct {
			CreateUpdateUser struct {
				Username   string
				Attributes []struct {
					Value string `json:"value"`
				}
			}
		}
	}
	err = json.Unmarshal(body, &rsp)
	if err != nil {
		log.Print("Error: ", err)
		return userVisibilities, err
	}
	for _, item := range rsp.Data.CreateUpdateUser.Attributes {
		if strings.HasPrefix(item.Value, "LIST") {
			userVisibilities = append(userVisibilities, item.Value)
		}
	}
	return userVisibilities, nil
}

// Name:        getAllActiveVisibilities
// Description: Get all of the active visibilities
// Inputs:
// Outputs:     activeVisibilities []string - All of the active visibilities
func getAllActiveVisibilities() (activeVisibilities []string, err error) {

	reqBody := "{\"query\":\"{attributesActive}\"}"

	body, err := runRouCurlCmd(reqBody)
	if err != nil {
		log.Print("Error: ", err)
		return activeVisibilities, err
	}

	var rsp struct {
		Data struct {
			AttributesActive []string
		}
	}
	err = json.Unmarshal(body, &rsp)
	if err != nil {
		log.Print("Error: ", err)
		return activeVisibilities, err
	}

	for _, attribute := range rsp.Data.AttributesActive {
		activeVisibilities = append(activeVisibilities, attribute)
	}

	return activeVisibilities, nil
}

// Name:        listPodsCmd
// Description: List pods by label selector
// Inputs:      label string - The label selector
// Outputs:     body []byte
//              err error
func listPodsCmd(label string) (body []byte, err error) {
	return runCurlCmd("GET", fmt.Sprintf("/api/v1/namespaces/%s/pods?labelSelector=%s", _namespace, label))
}

// Name:        listPodCmd
// Description: List pods by name
// Inputs:      name string - The name of the pod
// Outputs:     body []byte
//              err error
func listPodCmd(name string) (body []byte, err error) {
	return runCurlCmd("GET", fmt.Sprintf("/api/v1/namespaces/%s/pods/%s", _namespace, name))
}

// Name:        listServicesCmd
// Description: List the services by name
// Inputs:      name string - The name of the service
// Outputs:     body []byte
//              err error
func listServicesCmd(name string) (body []byte, err error) {
	return runCurlCmd("GET", fmt.Sprintf("/api/v1/namespaces/%s/services/%s", _namespace, name))
}

// Name:        deleteCmd
// Description: Delete resources in a namespace
// Inputs:      resource string - The type of resource to delete
//              name string - The name of the resource to delete
// Outputs:     body []byte
//              err error
func deleteCmd(resource string, name string) (body []byte, err error) {
	return runCurlCmd("DELETE", fmt.Sprintf("/api/v1/namespaces/%s/%s/%s", _namespace, resource, name))
}

// Name:        deleteClusterScopedCmd
// Description: Delete resources in the cluster scope
// Inputs:      resource string - The type of resource to delete
//              name string - The name of the resource to delete
// Outputs:     body []byte
//              err error
func deleteClusterScopedCmd(resource string, name string) (body []byte, err error) {
	return runCurlCmd("DELETE", fmt.Sprintf("/api/v1/%s/%s", resource, name))
}

// Name:        runCurlCmd
// Description: Run a cURL command
// Inputs:      method string - The type of HTTP method
//              path string - The subdirectory
// Outputs:     body []byte
//              err error
func runCurlCmd(method string, path string) (body []byte, err error) {
	serviceToken, err := ioutil.ReadFile("/var/run/secrets/kubernetes.io/serviceaccount/token")
	if err != nil {
		log.Fatal(err)
	}

	log.Print(fmt.Sprintf("Executing cULR command '%s' with path '%s'", method, path))

	req, err := http.NewRequest(method, _kube_admin_url+path, nil)
	req.Header.Add("Authorization", "Bearer "+string(serviceToken))
	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	client := &http.Client{Transport: tr}
	resp, err := client.Do(req)
	if err != nil {
		log.Print("Error on response:", err)
	}
	defer resp.Body.Close()
	body, err = ioutil.ReadAll(resp.Body)
	if err != nil {
		log.Print("Error while reading response bytes:", err)
	}
	log.Print(string([]byte(body)))

	return
}

// Name:        runRouCurlCmd
// Description: Run a cURL command against the ROU
// Inputs:      reqBody string - The request to send
// Outputs:     body []byte
//              err error
func runRouCurlCmd(reqBody string) (body []byte, err error) {
	log.Print(fmt.Sprintf("ROU Query: %s", reqBody))

	req, err := http.NewRequest("POST", _rules_of_use_url+"/graphql", strings.NewReader(reqBody))
	req.Header.Add("Content-Type", "application/json")
	req.Header.Add("Authorization", "dp-rou-key")

	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	client := &http.Client{Transport: tr}
	resp, err := client.Do(req)
	if err != nil {
		log.Print("Error on response:", err)
	}
	defer resp.Body.Close()
	body, err = ioutil.ReadAll(resp.Body)
	if err != nil {
		log.Print("Error while reading response bytes:", err)
	}
	log.Print(string([]byte(body)))

	return
}

// Name:        runApiCurlCmd
// Description: Run a cURL command against the API
// Inputs:      path string - The subdirectory
// Outputs:     body []byte
//              err error
func runApiCurlCmd(path string, attributesToReject []string) (body []byte, err error) {
	log.Printf("RunApiCurlCmd()")
	req, err := http.NewRequest("GET", _api_datasets+path, nil)

	attributesToRejectString := fmt.Sprintf("[\"%s\"]", strings.Join(attributesToReject, "\",\""))

	req.Header.Add("Content-Type", "application/json")
	req.Header.Add("X-Username", _api_user_name)
	req.Header.Add("X-Api-Key", _api_token)
	req.Header.Add("X-Attributes-To-Reject", attributesToRejectString)

	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	client := &http.Client{Transport: tr}
	resp, err := client.Do(req)
	if err != nil {
		log.Print("Error on response:", err)
	}
	defer resp.Body.Close()
	body, err = ioutil.ReadAll(resp.Body)

	if err != nil {
		log.Print("Error while reading response bytes:", err)
	}
	log.Print(string([]byte(body)))

	return
}

// Name:        getUsernameFromPodName
// Description: Get the username from the pod name
// Inputs:      name string - The name of the pod
// Outputs:     string - The username
func getUsernameFromPodName(name string) string {
	return strings.Replace(name, "spark-sql-", "", 1)
}

// Name:        contains
// Description: Determine if an array contains an item
// Inputs:      slice []string - An array of string elements
//              name string - The string to test
// Outputs:     bool - true if the given string is in the array; false otherwise
func contains(slice []string, item string) bool {
	set := make(map[string]struct{}, len(slice))
	for _, s := range slice {
		set[s] = struct{}{}
	}

	_, ok := set[item]
	return ok
}

// Name:        check
// Description: Panic if an error exists
// Inputs:      err error
// Outputs:
func check(err error) {
	if err != nil {
		panic(err)
	}
}

// Name:        isEnvVarSet
// Description: Panic if an ENV var is not set
// Inputs:      isSet bool - Is the ENV var set
// Outputs:
func isEnvVarSet(env_var string, isSet bool) {
	if !isSet {
		panic(fmt.Sprintf("The environment variable %s is not set", env_var))
	}
}

func main() {
	log.Print("Spark SQL Controller")

	// Get namespace of current container
	namespace, err := ioutil.ReadFile("/var/run/secrets/kubernetes.io/serviceaccount/namespace")
	check(err)
	_namespace = string(namespace)
	log.Printf("Current Namespace: %s", _namespace)

	// Set global vars from ENV vars
	varSet := false
	_rules_of_use_url, varSet = os.LookupEnv(_rou_api_http_env)
	isEnvVarSet(_rou_api_http_env, varSet)
	log.Printf(fmt.Sprintf("Env var '%s': %s", _rou_api_http_env, _rules_of_use_url))

	_api_datasets, varSet = os.LookupEnv(_api_http_env)
	isEnvVarSet(_api_http_env, varSet)
	log.Printf(fmt.Sprintf("Env var '%s': %s", _api_http_env, _api_datasets))

	_api_user_name, varSet = os.LookupEnv(_spark_sql_api_username)
	isEnvVarSet(_api_user_name, varSet)
	log.Printf(fmt.Sprintf("Env var '%s': %s", _spark_sql_api_username, _api_user_name))

	_api_token, varSet = os.LookupEnv(_spark_sql_api_key_env)
	isEnvVarSet(_spark_sql_api_key_env, varSet)

	_admin_username, varSet = os.LookupEnv(_spark_sql_admin_username)
	isEnvVarSet(_spark_sql_admin_username, varSet)

	_admin_password, varSet = os.LookupEnv(_spark_sql_admin_password)
	isEnvVarSet(_spark_sql_admin_password, varSet)

	// Listen and serve
	handleRequests()
}
