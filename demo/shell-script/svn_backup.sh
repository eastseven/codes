
var_date=`date "+%Y%m%d%H%M%S"`

echo '���ݿ�ʼ'

svnadmin dump /usr/svnroot/quickride > quickride.$var_date

echo '�������'

echo 'ѹ�������ļ���ʼ'

zip quickride.$var_date.zip quickride.$var_date
#tar -cvf quickride.$var_date.tar quickride.$var_date

echo 'ѹ�������ļ����'

rm -rf quickride.$var_date
echo 'ɾ��quickride.{$var_date}�ļ�'
