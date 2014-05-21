coqqueryhttpserver
==================

For a high level decription of what this server does, visit:
http://www.cs.cornell.edu/~aa755/software.php (2nd item in the list)



Usage:

1) build this project in netbeans (tested in Netbeans 7.4). then go to the generated dist/ directory.

2) java -jar CoqServerQuery.jar CONFIGFILE


The contents of CONFIGFILE must be as follows:

1) The first line must be the PORT number at which this server should listen

2) The second line should be a number which indicates the maximum number of http requests (across all projects)
    that can be pending when the server accepts a new http request

3) Then, there should be one line for each project that can be queried.
    Items in these lines are separated by a semicolon. The server starts an instance of coqtop for each of these projects
    
  a) The first item must be the path of the master Coq file of this project. The ".v" extension is supposed to be omitted.
       Suppose this item is of the format PATH/FILENAME. A new coqtop will be started in the directory PATH and
       the commant "Require Export FILENAME." will be sent to this coqtop process. The file FILENAME.v should
       import (Require Import/Export) all other Coq files of this project whose contents are supposed to be queried.
       
  b) The second item is used to determine the URL at which this project can be queried. If this item is CONTEXT,
      the webpage for querying this project is http://hostname:PORT/CONTEXT .
      
      
      
An example configFile is can be found at http://www.cs.cornell.edu/~aa755/cqQueryConfig.txt
The server corresponding to the first project can be accessed at
http://nuprl.cs.cornell.edu:4987/coq_query

       
    
