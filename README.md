coqqueryhttpserver
==================

An HTTP server that answers Coq queries like Print, Locate by talking to coqtop.
Usage:
1) build this project in netbeans (tested in Netbeans 7.4). 
2) copy the contents dist/ folder to the folder where the target .vo file containing all definitions is located. 
    cd to that folder.
3) java -jar CoqServerQuery.jar "Require Export target.vo."
