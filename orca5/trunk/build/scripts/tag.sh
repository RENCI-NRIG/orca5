base="https://geni-orca.renci.org/svn/orca/"

list="controllers/ben controllers/jaws controllers/sge drivers/iscsi drivers/localdisk drivers/machine drivers/network drivers/nfs drivers/sample drivers/vmm handlers/ec2 handlers/standard manage/boot manage/imagemanager manage/standard build-all core nodeagent nodeagenthost site webapp tests/core tests/unit tools/axis2repository tools/cmdline tools/config tools/dependencies tools/drivers tools/site-skin"

tag=1.1-alpha

list="handlers/ec2 tools/axis2repository"
for x in $list; do
	echo processing $x
	svn import tags $base/$x/tags -m "creating tags directory"
	svn import branches $base/$x/branches -m "creating branches directory"

	svn cp $base/$x/trunk $base/$x/tags/$tag -m "tagging the current code version (1.1-alpha)"
	svn cp $base/$x/trunk $base/$x/branches/$tag -m "creating maintenance branch for 1.1-alpha"
done

