USE orca;

--
-- Dumping data for table `Machines`
--

delete from StorageServerActors;
delete from Machines;
delete from StorageServers;


/*!40000 ALTER TABLE `Machines` DISABLE KEYS */;

LOCK TABLES `Machines` WRITE;
INSERT INTO `Machines` VALUES
(20000,'go-1.duke','go-1.duke','eec27d5c-c9ad-4186-9432-16950e92dbc2','192.168.202.11', 2047, 1, 2, NULL, NULL),
(20001,'go-2.duke','go-2.duke', '5df33af9-ddae-48cb-bb19-369fb3b1aeef','192.168.202.12', 2047, 1, 2, NULL, NULL);


UNLOCK TABLES;
/*!40000 ALTER TABLE `Machines` ENABLE KEYS */; 

--
-- Dumping data for table `StorageServers`
--

/*!40000 ALTER TABLE `StorageServers` DISABLE KEYS */;
LOCK TABLES `StorageServers` WRITE;
INSERT INTO `StorageServers` VALUES
(20000,'go-nfs.duke','3b171f41-c84e-4c05-b9b2-c8d6e9e47209',2,'192.168.201.16','rpool/images/orca/','192.168.202.12',NULL);

UNLOCK TABLES;
/*!40000 ALTER TABLE `StorageServers` ENABLE KEYS */;

--
-- Dumping data for table `Devices`
--

/*!40000 ALTER TABLE `Devices` DISABLE KEYS */;
LOCK TABLES `Devices` WRITE;
INSERT INTO `Devices` VALUES
(20000,'6509.duke','b317da71-92ab-45bd-8583-794d9a6e347f',1,'192.168.202.7','192.168.202.12',NULL);
UNLOCK TABLES;

