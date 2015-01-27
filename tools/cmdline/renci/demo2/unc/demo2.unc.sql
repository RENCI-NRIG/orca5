USE orca;

--
-- Dumping data for table `Machines`
--

delete from StorageServerActors;
delete from Machines;
delete from StorageServers;
delete from DeviceActors;
delete from Devices;

/*!40000 ALTER TABLE `Machines` DISABLE KEYS */;

LOCK TABLES `Machines` WRITE;
INSERT INTO `Machines` VALUES
(30000,'go-1.unc','go-1.unc', '2bb1132c-be34-4b92-b518-1849c2d753a1','192.168.203.11', 2047, 1, 2, NULL, NULL),
(30001,'go-2.unc','go-2.unc', '34c93547-7c44-4317-9ab0-e019e4822d02','192.168.203.12', 2047, 1, 2, NULL, NULL);


UNLOCK TABLES;
/*!40000 ALTER TABLE `Machines` ENABLE KEYS */; 

--
-- Dumping data for table `StorageServers`
--

/*!40000 ALTER TABLE `StorageServers` DISABLE KEYS */;
LOCK TABLES `StorageServers` WRITE;
INSERT INTO `StorageServers` VALUES
(30000,'go-nfs.unc','3b171f41-c84e-4c05-b9b2-c8d6e9e47209',2,'192.168.201.16','rpool/images/orca/','192.168.203.12',NULL);

UNLOCK TABLES;
/*!40000 ALTER TABLE `StorageServers` ENABLE KEYS */;

