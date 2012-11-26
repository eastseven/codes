#!/bin/bash

echo "export quickridetest data"
var_date=`date "+%Y%m%d%H%M%S"`
echo $var_date

exp quickridetest/quickridetest@dev file=quickridetest-$var_date.dmp owner=quickridetest

SQLPLUS=` which sqlplus`
$SQLPLUS /nolog << EOF
conn /as sysdba
drop user QUICKRIDEDEV cascade;
create user quickridedev identified by quickridedev;
grant connect, resource, dba to quickridedev;

EOF

imp quickridedev/quickridedev@dev file=quickridetest-$var_date.dmp fromuser=quickridetest touser=quickridedev
echo "done..."
