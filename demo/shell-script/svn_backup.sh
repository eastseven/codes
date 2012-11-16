
var_date=`date "+%Y%m%d%H%M%S"`

echo '备份开始'

svnadmin dump /usr/svnroot/quickride > quickride.$var_date

echo '备份完成'

echo '压缩备份文件开始'

zip quickride.$var_date.zip quickride.$var_date
#tar -cvf quickride.$var_date.tar quickride.$var_date

echo '压缩备份文件完成'

rm -rf quickride.$var_date
echo '删除quickride.{$var_date}文件'
