USE `orca`;

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
(1,1,'3b171f41-c84e-4c05-b9b2-c8d6e9e47209');
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
