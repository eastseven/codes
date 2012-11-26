#!/bin/bash

var_date=`date "+%Y%m%d%H%M%S"`

echo $var_date

exp quickrideproduct/quickrideproduct file=quickrideproduct-$var_date.dmp owner=quickrideproduct

echo 'database backup done...'

zip quickrideproduct$var_date.zip quickrideproduct-$var_date.dmp

echo 'zip database backup done...'

scp -P 2222 -r quickrideproduct$var_date.zip root@183.221.125.150:/root/quickrideproduct

echo 'transfor the database file to si chuan 100 server...'
