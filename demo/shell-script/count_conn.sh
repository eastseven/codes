#!/bin/sh
netstat -anp | grep :$1 | awk '{print $5}' | awk -F: '{print $1}' | sort | uniq -c
