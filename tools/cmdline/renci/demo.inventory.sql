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
(10000,'go-1.renci','go-1.renci', 'f1e0c4e1-942e-4b31-8515-3f53e940dd44','192.168.201.12', 2047, 1, 2, NULL, NULL),
(10001,'go-2.renci','go-2.renci', '987ee7fa-a371-4e05-9054-7e62cc002741','192.168.201.14', 2047, 1, 2, NULL, NULL),
(10002,'go-3.renci','go-3.renci', '1cddeb27-b000-4dbf-822a-bf5779d7d24f','192.168.201.15', 2047, 1, 2, NULL, NULL),
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
(10000,'go-nfs.renci','3b171f41-c84e-4c05-b9b2-c8d6e9e47209',2,'192.168.201.16','rpool/images/orca/','192.168.201.12',NULL),
(20000,'go-nfs.duke','3b171f41-c84e-4c05-b9b2-c8d6e9e47209',2,'192.168.201.16','rpool/images/orca/','192.168.202.12',NULL);

UNLOCK TABLES;
/*!40000 ALTER TABLE `StorageServers` ENABLE KEYS */;

--
-- Dumping data for table `Devices`
--

/*!40000 ALTER TABLE `Devices` DISABLE KEYS */;
LOCK TABLES `Devices` WRITE;
INSERT INTO `Devices` VALUES
(10000,'6509.renci','afcdb724-e20f-4e62-bdae-ad80c113c7c2',1,'192.168.201.8','192.168.201.12',NULL),
(20000,'6509.duke','b317da71-92ab-45bd-8583-794d9a6e347f',1,'192.168.202.7','192.168.202.12',NULL);
UNLOCK TABLES;
/*!40000 ALTER TABLE `Devices` ENABLE KEYS */;

