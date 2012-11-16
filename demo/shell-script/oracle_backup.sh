#!/bin/bash

var_date=`date "+%Y%m%d%H%M%S"`

echo $var_date

exp quickrideproduct/quickrideproduct file=quickrideproduct-$var_date.dmp owner=quickrideproduct

echo 'database backup done...'
