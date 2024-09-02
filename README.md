# PATEN - A Project to Detect Vulnerable Third-party Library APIs

This is the documentation for the project. It can be run after compilation, or you can directly invoke it from the jar package.



**jar execution command**：

```shell
java -jar PATEN.jar <target_file> <output_folder>
java -jar PATEN.jar /home/experiment/experiment_dataset/SNYK-JAVA-CNFXBINBUBBLE-1300551/influenced_package/13d880/DoubleJwt.java/bubble-fireworks-plugin-token-0.0.9-RELEASE.jar/tfile.java /root/Experiment
```



## Prerequisites

Before running, ensure that the database has been built (import `vul_database_exp.sql`) and configured correctly.

Configuration information is located in the `./src/Configure` directory, which includes three database tables:

- **vul_apis**: Stores basic information about vulnerabilities, file locations, and locations of fixed files.
- **fileast**: Stores the AST trees of files.
- **changelist**: Stores the locations of AST subtrees that have been modified during vulnerability fixes, indicating whether the changes are additions, deletions, or modifications of nodes or subtrees.

`RuntimeConfig` is used to specify the locations for runtime temporary files and target files.

The `experiment_dataset` (available at https://github.com/PATEN-Tool/PATEN_Evaluation_TPLs) is recommended to be placed under `/home/experiment/experiment_dataset`.

## Main Process

The main process consists of two parts: locating suspicious methods from the input JAR file and the method feature matching phase. The main class is `./src/test/runExp.java`, where the main method implements the locating function. If suspicious methods are found, it transitions to the matching phase.

1. **Locating Phase**:

   The initial input is the location of the JAR file. It sequentially decompresses the file, uses ASM to retrieve the full method names of all class files, and checks the database for corresponding vulnerability entries. If a potential vulnerability is found, it decompiles and extracts the relevant methods (default saved as `./runtimeFile/tfile.java`) and proceeds to the matching phase; otherwise, it continues to the next method.

2. **Matching Phase**:

   This phase primarily implements the code matching section. The inputs for this phase are: filename, commit ID, and the full method name (e.g., `SingleJwt.java 13d880 cn.fxbin.bubble.plugin.token.SingleJwt.parseToken`).

## Output Results

The output consists of matching results, saved in the specified output file in JSON format. The main result fields include:

- **Cost**: The runtime duration.
- **Analysis Res**: The analysis result, where `true` indicates a vulnerability is present, `false` indicates no vulnerability, `nochange` indicates the vulnerability fix has no effective modification to the method, and `Unknown` (No target Method) indicates the method in the target file does not match the method being compared.
- **NormalizedDistance**: Indicates the similarity of the contextual parts of the target file and the vulnerability file when uncertain; smaller values indicate greater similarity and can serve as auxiliary judgment.
- **VT Edit Distance**: Represents the distance between vulnerability features and target features (0-1).
- **PT Edit Distance**: Represents the distance between fix features and target features (0-1).

Input files for the matching phase can also be obtained through other preprocessing methods. If `res.json` does not exist or is empty after execution, it indicates that no vulnerable methods were found.

## Database Table Structure

### vul_apis

(Storing basic information about vulnerabilities, as well as file locations and fixed files)

```
+------------------+--------------+------+-----+---------+-------+
| Field            | Type         | Null | Key | Default | Extra |
+------------------+--------------+------+-----+---------+-------+
| snyk_id          | varchar(300) | YES  |     | NULL    |       |
| cve_no           | varchar(300) | YES  |     | NULL    |       |
| group_id         | varchar(300) | YES  |     | NULL    |       |
| artifact_id      | varchar(300) | NO   | PRI | NULL    |       |
| affected_version | varchar(600) | YES  |     | NULL    |       |
| git_repo         | varchar(600) | YES  |     | NULL    |       |
| commitid         | varchar(300) | YES  |     | NULL    |       |
| filename         | varchar(300) | YES  |     | NULL    |       |
| file_longname    | varchar(600) | NO   | PRI | NULL    |       |
| methodname       | varchar(300) | YES  |     | NULL    |       |
| method_longname  | varchar(300) | NO   | PRI | NULL    |       |
| classname        | varchar(300) | YES  |     | NULL    |       |
| params           | varchar(600) | NO   | PRI | NULL    |       |
| del_lines        | varchar(600) | NO   | PRI | 0       |       |
| add_lines        | varchar(600) | NO   | PRI | 0       |       |
| MD5              | varchar(300) | YES  |     | NULL    |       |
| vfile            | longtext     | YES  |     | NULL    |       |
| pfile            | longtext     | YES  |     | NULL    |       |
+------------------+--------------+------+-----+---------+-------+
```



To expand the vulnerability database, you need to collect `vul_apis` information yourself. This project can generate the corresponding abstract syntax tree database in the `fileast` table and the `changelist` table, structured as follows:

### fileast

(Stores the AST trees of files)

```
+-----------------+--------------+------+-----+---------+-------+
| Field           | Type         | Null | Key | Default | Extra |
+-----------------+--------------+------+-----+---------+-------+
| filename        | varchar(100) | NO   | PRI | NULL    |       |
| commitid        | varchar(100) | NO   | PRI | NULL    |       |
| vfile           | longtext     | YES  |     | NULL    |       |
| pfile           | longtext     | YES  |     | NULL    |       |
| vastxml         | longtext     | YES  |     | NULL    |       |
| pastxml         | longtext     | YES  |     | NULL    |       |
| method_longname | varchar(300) | NO   | PRI | NULL    |       |
+-----------------+--------------+------+-----+---------+-------+
```



### changelist

(Records changes in files)

(Stores the locations of AST subtrees that have been modified during vulnerability fixes, indicating whether the changes are additions or deletions, and whether they are nodes or subtrees)

```
+-----------------+--------------+------+-----+---------+-------+
| Field           | Type         | Null | Key | Default | Extra |
+-----------------+--------------+------+-----+---------+-------+
| filename        | varchar(100) | NO   | PRI | NULL    |       |
| commitid        | varchar(100) | NO   | PRI | NULL    |       |
| type            | varchar(10)  | NO   | PRI | NULL    |       |
| Optype          | varchar(20)  | NO   | PRI | NULL    |       |
| subtreeSeq      | varchar(300) | NO   | PRI | NULL    |       |
| method_longname | varchar(300) | NO   | PRI | NULL    |       |
+-----------------+--------------+------+-----+---------+-------+
```

