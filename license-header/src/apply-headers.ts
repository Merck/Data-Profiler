/*
  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
 
 	Licensed to the Apache Software Foundation (ASF) under one
 	or more contributor license agreements. See the NOTICE file
 	distributed with this work for additional information
 	regarding copyright ownership. The ASF licenses this file
 	to you under the Apache License, Version 2.0 (the
 	"License"); you may not use this file except in compliance
 	with the License. You may obtain a copy of the License at
 
 	http://www.apache.org/licenses/LICENSE-2.0
 
 
 	Unless required by applicable law or agreed to in writing,
 	software distributed under the License is distributed on an
 	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 	KIND, either express or implied. See the License for the
 	specific language governing permissions and limitations
 	under the License.
*/
import fs from 'fs';
import path from 'path';


/**
 * recurse through directories, looking for files names and extensions
 */
class DirectoryTraverser {
    static EXCLUDES = ["acumulo-export",  "dp-core", "__init__.py", ".ipynb", ".txt", ".md", "build", "dist", 
        ".http", ".cfg", "lib", ".xml","node_modules", "backups", ".bin", "target", "compile", "venv", 
        ".idea", ".vscode", ".gradle", ".terraform", ".git", ".gz", ".bz2", ".jar", ".whl", ".md", ".csv", ".env", ".iml"]
    // static INCLUDES = ["Dockerfile", ".py", ".sh", ".js", ".ts", ".tsx", ".jsx", ".tf", ".yml", ".yaml"]
    static HEADER = "dist/assets/header.txt"

    // EXCLUDE_REGEXS: RegExp[]
    // INCLUDE_REGEXS: RegExp[]
    fileCommenters: Record<string, Commenter>
    licenseHeader: string
    filesUpdated: number

    constructor() {
        // this.EXCLUDE_REGEXS = DirectoryTraverser.EXCLUDES.map(pattern => this.toRegexp(pattern))
        // this.INCLUDE_REGEXS = DirectoryTraverser.INCLUDES.map(pattern => this.toRegexp(pattern))

        const hashCommenter = new Commenter("#", "#", "#")
        const jsCommenter = new Commenter("/*", " ", "*/")
        const javaCommenter = new Commenter("/**", "* ", "**/")
        const pyCommenter = new Commenter('"""', " ", '"""')
        this.fileCommenters = {
            ".yml": hashCommenter,
            ".yaml": hashCommenter,
            ".sh": hashCommenter,
            "Dockerfile": hashCommenter,
            ".py": pyCommenter,
            ".tf": jsCommenter,
            ".js": jsCommenter,
            ".ts": jsCommenter,
            ".jsx": jsCommenter,
            ".tsx": jsCommenter,
            ".java": javaCommenter,
            ".graphqls": hashCommenter
        }

        this.licenseHeader = this.loadFile(DirectoryTraverser.HEADER)
        // console.log("using license header:")
        // console.log(this.licenseHeader)
        this.filesUpdated = 0
    }

    traverse(dir: Readonly<string>): string[] {
        if (dir === undefined || this.isEmpty(dir.toString()) || !this.isValidToTraverse(dir) || this.isExcludePattern(dir)) {
            return []
        }
        const paths = fs.readdirSync(dir)
            .filter(el => !this.isExcludePattern(el))
            .map(el =>  path.resolve(dir, el))

        const files = paths
            .filter(el => this.isValidToApply(el))
            .map(el => this.applyHeader(el))
        if (files && files.length > 0) {
            // console.log(`found ${files.length} files`)
            this.filesUpdated += files.length
        }

        const dirs = paths
            .filter(el => this.isValidToTraverse(el) && !this.isExcludePattern(el))

        // if (dirs && dirs.length > 0) {
        //     // console.log(`found ${dirs.length} dirs`)
        // }
    
        return [...dirs, ...dirs.flatMap(el => this.traverse(el))]
    }
    
    applyHeader(path: Readonly<string>): void {
        if (!this.isValidToApply(path) && !this.isExcludePattern(path)) {
            return
        }


        const commenter = this.fetchCommenter(path)
        if (commenter === undefined) {
            return
        }

        const header = commenter?.generateComment(this.licenseHeader)
        console.log("apply header on: " + path)
        // console.log(header)
        // console.log(" ")

        const contents = this.loadFile(path)
        if (contents.startsWith(header)) {
            console.log("file: " + path + " already has a header, skipping...")
            return 
        }

        this.overwriteFile(path, header + "\n" + contents)
    }

    fetchCommenter(path: Readonly<string>): Commenter|undefined {
        if (!path || this.isEmpty(path)) {
            return undefined
        }

        const foundKey =  Object.keys(this.fileCommenters)
            .find((key) => path.endsWith(key) || path === key)
        
        return foundKey !== undefined ? this.fileCommenters[foundKey] : undefined
    }

    isValidToTraverse(path: Readonly<string>): boolean {
        if (!path || this.isEmpty(path) || !fs.existsSync(path)) {
            return false
        }
        const stats = fs.statSync(path)
        return stats && !stats.isSymbolicLink() && stats.isDirectory()
    }

    isValidToApply(path: Readonly<string>): boolean {
        if (!path || this.isEmpty(path) || !fs.existsSync(path)) {
            return false
        }
        const stats = fs.statSync(path)
        return stats && !stats.isSymbolicLink() && stats.isFile()
    }
    
    isExcludePattern(path: Readonly<string>): boolean {
        // return this.EXCLUDE_REGEXS.find(pattern => pattern.test(path)) !== undefined
        return DirectoryTraverser.EXCLUDES.find(pattern => path.endsWith(pattern) || path === pattern) !== undefined
    }
    
    isEmpty(str: Readonly<string>): boolean {
        return str.length === 0 
    }
    
    toRegexp(pattern: Readonly<string>): RegExp {
        // const escaped = pattern.replace("*", "\\*")
        return new RegExp(pattern + "$")
    }

    loadFile(file: Readonly<string>): string {
        if (!file || this.isEmpty(file)) {
            return ""
        }

        return fs.readFileSync(file, 'utf-8').toString()
    }

    overwriteFile(file: Readonly<string>, contents: Readonly<string>): void {
        fs.writeFileSync(file, contents, {
            encoding: 'utf-8',
            flag: 'w'
        })
    }

}


/**
 * class to generate a comment header
 */
const NEWLINE = '\n'
class Commenter {
    commentStart: string
    lineStart: string
    commentEnd: string

    constructor(start: Readonly<string>, line: Readonly<string>, 
        end:  Readonly<string>) {
        this.commentStart = start
        this.lineStart = line
        this.commentEnd = end
    }

    generateComment(comment:  Readonly<string>): string {
        const lines = comment.split(NEWLINE).map(line => this.lineStart + line)
        return [this.commentStart, ...lines, this.commentEnd].join(NEWLINE)
    }
}

function main(root: Readonly<string>): void {
    console.log("traverse: " + root)
    const traverser = new DirectoryTraverser()
    const dirs = traverser.traverse(path.resolve(root))
    console.log(`${dirs.length} total dirs visited`)
    console.log(`files updated: ${traverser.filesUpdated}`)
}

// npm run clean && npm run compile && npm run headers
const argv = process.argv
main(argv[2])