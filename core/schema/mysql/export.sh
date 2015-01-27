#!/bin/bash

echo "Exporting the whole database..."
echo "USE orca;" > current.sql
mysqldump -u orca orca >> current.sql
echo "Done"

echo "Exporting orca schema..."
echo "USE orca;" > full.schema.sql
mysqldump -u orca -d orca >> full.schema.sql
mysqldump -u orca -d orca > full.schema.noheader.sql
echo "Done"

#echo "Exporting orca data..."
#echo "USE orca;" > full.data.sql
#mysqldump -u orca -t orca MachineTypes StorageProtocols StorageServerTypes Roles >> full.data.sql
#mysqldump -u orca -t orca MachineTypes StorageProtocols StorageServerTypes Roles >> full.data.noheader.sql
#echo "Done"

echo "Exporting test schema..."
echo "USE orca_test;" > test.schema.sql
mysqldump -u orca -d orca >> test.schema.sql
echo "Done"

#echo "Exporting test data..."
#echo "USE orca_test;" > test.data.sql
#mysqldump -u orca -t orca MachineTypes StorageProtocols StorageServerTypes >> test.data.sql
#echo "Done"
