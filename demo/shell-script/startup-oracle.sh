#!/bin/bash

SQLPLUS=` which sqlplus`
$SQLPLUS /nolog << EOF
conn /as sysdba
startup
quit
EOF

lsnrctl start

emctl start dbconsole
