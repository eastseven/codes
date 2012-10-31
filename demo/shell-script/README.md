简单的监视某个端口的连接数的Linux命令

首先创建一个脚本文件, 我们叫他count_conn吧

#!/bin/sh
netstat -anp | grep :$1 | awk '{print $5}' | awk -F: '{print $1}' | sort | uniq -c

然后执行命令:

watch -n 1 count_conn 8080

最后的参数就是你要监视的端口.