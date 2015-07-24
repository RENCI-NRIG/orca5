USE orca;

LOCK TABLES `Inventory` WRITE;
/*!40000 ALTER TABLE `Inventory` DISABLE KEYS */;
INSERT INTO `Inventory` VALUES (1,'demo1', '172.16.1.11', '172.16.1.11', '8db398aa-1396-11df-ba24-000c29b1c193',1, 2, NULL);
INSERT INTO `Inventory` VALUES (2,'demo2', '172.16.1.12', '172.16.1.12', '52a86e7e-1397-11df-95b3-000c29b1c193',1, 2, NULL);
INSERT INTO `Inventory` VALUES (3,'demo3', '172.16.1.14', '172.16.1.13', '7704469e-1397-11df-9010-000c29b1c193',1, 2, NULL);
INSERT INTO `Inventory` VALUES (4,'demo4', '172.16.1.14', '172.16.1.14', '8468fa28-1397-11df-8260-000c29b1c193',1, 2, NULL);
/*!40000 ALTER TABLE `Inventory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Machines`
--


/*!40000 ALTER TABLE `Machines` DISABLE KEYS */;
LOCK TABLES `Machines` WRITE;
INSERT INTO `Machines` VALUES
(1,'demo001','demo001', '583c10bfdbd326ba:-69de3b5c:114d6677bf2:-8000','192.168.0.10', 2048, 1, 2, NULL, NULL),
(2,'demo002','demo002', '583c10bfdbd326ba:5bb71374:114d6687a2b:-8000','192.168.0.11', 2048, 1, 2, NULL, NULL),
(3,'demo003','demo003', '583c10bfdbd326ba:de47c07:114d668fcf3:-8000','192.168.0.12', 2048, 1, 2, NULL, NULL),
(4,'demo004','demo004', '583c10bfdbd326ba:732c8582:114d6696895:-8000','192.168.0.13', 2048, 1, 2, NULL, NULL);
UNLOCK TABLES;
/*!40000 ALTER TABLE `Machines` ENABLE KEYS */;

/*!40000 ALTER TABLE `StorageServers` DISABLE KEYS */;
LOCK TABLES `StorageServers` WRITE;
INSERT INTO `StorageServers` VALUES
(1,'demo-iscsi-server','583c10bfdbd326ba:-5443ba24:1168c1705a2:-8000',1,'10.10.10.1','/vol/vol0/iscsi','10.10.0.1',NULL),
(2,'demo-zfs-server','583c10bfdbd326ba:-6692ff28:1168c1874d3:-8000',2,'10.10.10.2','sata/images/shirako/','10.10.0.1',NULL);
UNLOCK TABLES;
/*!40000 ALTER TABLE `StorageServers` ENABLE KEYS */;

--
-- Dumping data for table `Images`
--


/*!40000 ALTER TABLE `Images` DISABLE KEYS */;

delete from NfsImages;
delete from InstalledImages;
delete from Images;



LOCK TABLES `Images` WRITE;
INSERT INTO `Images` VALUES

(1,'b24aa725-f142-4c19-8e22-998bbf483f6a','Centos 5.2 minimal',
'',
'',
'A minimal installation of Centos 5.2.');


UNLOCK TABLES;
/*!40000 ALTER TABLE `Images` ENABLE KEYS */;

--
-- Dumping data for table `InstalledImages`
--

/*!40000 ALTER TABLE `InstalledImages` DISABLE KEYS */;
LOCK TABLES `InstalledImages` WRITE;
INSERT INTO `InstalledImages` VALUES
(1,1,'583c10bfdbd326ba:-6692ff28:1168c1874d3:-8000');
UNLOCK TABLES;
/*!40000 ALTER TABLE `InstalledImages` ENABLE KEYS */;

--
-- Dumping data for table `NfsImages`
--

/*!40000 ALTER TABLE `NfsImages` DISABLE KEYS */;
LOCK TABLES `NfsImages` WRITE;
INSERT INTO `NfsImages` VALUES
(1,1,'rpool/images/orca/','centos52@base');
UNLOCK TABLES;
/*!40000 ALTER TABLE `NfsImages` ENABLE KEYS */;
