#!/bin/bash

# mysql user name
user=root

# user name to be used by ORCA
ORCA_USER=orca
# user name password
ORCA_DB=orca

# executes the specified commands against MYSQL
function mysql_execute() {
	echo "$1" | mysql -u $user $pass
	if [ $? -ne 0 ]; then
		echo "Could not execute sql statement"
		exit 1
	fi
}

function check_access() {
	if [ -z $pass ]; then
		echo "Checking if mysql can be accessed: user=$user using password: false"
	else
		echo "Checking if mysql can be accessed: user=$user using password: true"
	fi

	out=`echo "show databases" | mysql -u $user $pass`
	if [ $? -ne 0 ]; then
		echo "Could not access MYSQL. Please invoke this script with the right arguments"
		exit 1
	fi
	echo "Acessing mysql succeeded"
}

function do_it () {
	check_access

	echo "Creating database: $ORCA_DB and database user: $ORCA_USER"
	mysql_execute "drop database if exists $ORCA_DB"
	if [ -z $ORCA_PASS ]; then
		mysql_execute "grant all privileges on $ORCA_DB.* to '${ORCA_USER}'@'localhost'" 
	else
		mysql_execute "grant all privileges on $ORCA_DB.* to '${ORCA_USER}'@'localhost' identifier by \'${ORCA_PASS}\'"
	fi
	mysql_execute "create database $ORCA_DB"
	echo "Database and user created successfully"

	echo "Creating schema"
	content=`cat full.schema.noheader.sql`
	mysql_execute "use $ORCA_DB; $content"
	echo "Database schema created successfully"

	echo "Populating database"
	content=`cat full.data.noheader.sql`
	mysql_execute "use $ORCA_DB; $content"
	echo "Database populated successfully"
}

function usage() {
	echo "This script configures mysql to be used by Orca"
	echo -e "Usage:\t[-u mysql_admin_user] [-p mysql_admin_password]"
	echo -e "\t[-d orca_db_name] [-U orca_user] [-P orca_password]"
	exit 1
}

# u - mysql admin user
# p - mysql admin user password
# d - orca database name
# U - orca database user
# P - orca database user password

while getopts ":u:p:d:U:P:" option
do
	case $option in
		u)
			user=$OPTARG
			;;
		p)
			pass=" --password=$OPTARG"
			;;
		d) 
			ORCA_DB=$OPTARG
			;;
		U)
			ORCA_USER=$OPTARG
			;;
		P)
			ORCA_PASS=$OPTARG
			;;
		* ) 
			usage
			;;
	esac
done
shift $(($OPTIND - 1))

if [ $# -ne 0 ]; then
	usage
fi

do_it

