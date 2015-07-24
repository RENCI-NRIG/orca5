USE orca;
-- MySQL dump 10.13  Distrib 5.5.28, for osx10.6 (i386)
--
-- Host: localhost    Database: orca
-- ------------------------------------------------------
-- Server version	5.5.28

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `Actors`
--

DROP TABLE IF EXISTS `Actors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Actors` (
  `act_id` int(11) NOT NULL AUTO_INCREMENT,
  `act_name` varchar(255) NOT NULL,
  `act_guid` varchar(255) NOT NULL,
  `act_type` int(11) NOT NULL,
  `shirakoproperties` longtext,
  PRIMARY KEY (`act_id`),
  UNIQUE KEY `act_name` (`act_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1474 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Clients`
--

DROP TABLE IF EXISTS `Clients`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Clients` (
  `clt_id` int(11) NOT NULL AUTO_INCREMENT,
  `clt_act_id` int(11) DEFAULT NULL,
  `clt_name` varchar(255) NOT NULL,
  `clt_guid` varchar(255) NOT NULL,
  `shirakoproperties` longtext,
  PRIMARY KEY (`clt_id`),
  UNIQUE KEY `clt_act_id` (`clt_act_id`,`clt_name`),
  CONSTRAINT `Clients_ibfk_1` FOREIGN KEY (`clt_act_id`) REFERENCES `Actors` (`act_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=303 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ConfigMappings`
--

DROP TABLE IF EXISTS `ConfigMappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ConfigMappings` (
  `cfgm_id` int(11) NOT NULL AUTO_INCREMENT,
  `cfgm_act_id` int(11) NOT NULL,
  `cfgm_type` varchar(255) NOT NULL,
  `cfgm_path` varchar(255) NOT NULL,
  `shirakoproperties` longtext,
  PRIMARY KEY (`cfgm_id`),
  KEY `cfgm_ibfk_1` (`cfgm_act_id`),
  CONSTRAINT `cfgm_ibfk_1` FOREIGN KEY (`cfgm_act_id`) REFERENCES `Actors` (`act_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=162 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Inventory`
--

DROP TABLE IF EXISTS `Inventory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Inventory` (
  `inv_id` int(11) NOT NULL AUTO_INCREMENT,
  `inv_name` varchar(255) NOT NULL,
  `inv_ip` varchar(255) DEFAULT NULL,
  `inv_control` varchar(128) DEFAULT NULL,
  `inv_uid` varchar(255) NOT NULL,
  `inv_type` smallint(6) DEFAULT '1',
  `inv_state` int(11) NOT NULL,
  `shirakoproperties` longtext,
  PRIMARY KEY (`inv_id`),
  UNIQUE KEY `inv_uid` (`inv_uid`),
  UNIQUE KEY `inv_name` (`inv_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `InventoryActors`
--

DROP TABLE IF EXISTS `InventoryActors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `InventoryActors` (
  `ina_id` int(11) NOT NULL AUTO_INCREMENT,
  `ina_inv_id` int(11) NOT NULL,
  `ina_act_id` int(11) NOT NULL,
  PRIMARY KEY (`ina_id`),
  UNIQUE KEY `ina_inv_id` (`ina_inv_id`,`ina_act_id`),
  KEY `ina_act_id` (`ina_act_id`),
  CONSTRAINT `InventoryActors_ibfk_1` FOREIGN KEY (`ina_inv_id`) REFERENCES `Inventory` (`inv_id`) ON DELETE CASCADE,
  CONSTRAINT `InventoryActors_ibfk_2` FOREIGN KEY (`ina_act_id`) REFERENCES `Actors` (`act_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `InventorySlices`
--

DROP TABLE IF EXISTS `InventorySlices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `InventorySlices` (
  `ins_id` int(11) NOT NULL AUTO_INCREMENT,
  `ins_inv_id` int(11) NOT NULL,
  `ins_slc_id` int(11) NOT NULL,
  PRIMARY KEY (`ins_id`),
  UNIQUE KEY `ins_inv_id` (`ins_inv_id`,`ins_slc_id`),
  KEY `ins_slc_id` (`ins_slc_id`),
  CONSTRAINT `InventorySlices_ibfk_1` FOREIGN KEY (`ins_inv_id`) REFERENCES `Inventory` (`inv_id`) ON DELETE CASCADE,
  CONSTRAINT `InventorySlices_ibfk_2` FOREIGN KEY (`ins_slc_id`) REFERENCES `Slices` (`slc_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ManagerObjects`
--

DROP TABLE IF EXISTS `ManagerObjects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ManagerObjects` (
  `mo_id` int(11) NOT NULL AUTO_INCREMENT,
  `mo_key` varchar(255) NOT NULL,
  `mo_act_id` int(11) DEFAULT NULL,
  `shirakoproperties` longtext,
  PRIMARY KEY (`mo_id`),
  UNIQUE KEY `mo_key` (`mo_key`),
  KEY `mo_act_id` (`mo_act_id`),
  CONSTRAINT `ManagerObjects_ibfk_1` FOREIGN KEY (`mo_act_id`) REFERENCES `Actors` (`act_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=678 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Miscellaneous`
--

DROP TABLE IF EXISTS `Miscellaneous`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Miscellaneous` (
  `msc_id` int(11) NOT NULL AUTO_INCREMENT,
  `msc_path` varchar(255) NOT NULL,
  `shirakoproperties` longtext,
  PRIMARY KEY (`msc_id`),
  UNIQUE KEY `msc_path` (`msc_path`)
) ENGINE=InnoDB AUTO_INCREMENT=367 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Packages`
--

DROP TABLE IF EXISTS `Packages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Packages` (
  `pkg_id` int(11) NOT NULL AUTO_INCREMENT,
  `pkg_guid` varchar(255) NOT NULL,
  `shirakoproperties` longtext,
  PRIMARY KEY (`pkg_id`),
  UNIQUE KEY `pkg_guid` (`pkg_guid`)
) ENGINE=InnoDB AUTO_INCREMENT=1581 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Plugins`
--

DROP TABLE IF EXISTS `Plugins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Plugins` (
  `plg_id` int(11) NOT NULL AUTO_INCREMENT,
  `plg_pkg_id` int(11) NOT NULL,
  `plg_local_id` int(11) NOT NULL,
  `plg_type` int(11) NOT NULL,
  `plg_actor_type` varchar(255) DEFAULT NULL,
  `shirakoproperties` longtext,
  PRIMARY KEY (`plg_id`),
  KEY `plg_ibfk_1` (`plg_pkg_id`),
  CONSTRAINT `plg_ibfk_1` FOREIGN KEY (`plg_pkg_id`) REFERENCES `Packages` (`pkg_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3281 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Proxies`
--

DROP TABLE IF EXISTS `Proxies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Proxies` (
  `prx_id` int(11) NOT NULL AUTO_INCREMENT,
  `prx_act_id` int(11) NOT NULL,
  `prx_name` varchar(255) NOT NULL,
  `shirakoproperties` longtext,
  PRIMARY KEY (`prx_id`),
  UNIQUE KEY `prx_name` (`prx_name`,`prx_act_id`),
  KEY `prx_ibfk_1` (`prx_act_id`),
  CONSTRAINT `prx_ibfk_1` FOREIGN KEY (`prx_act_id`) REFERENCES `Actors` (`act_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=303 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Reservations`
--

DROP TABLE IF EXISTS `Reservations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Reservations` (
  `rsv_id` int(11) NOT NULL AUTO_INCREMENT,
  `rsv_slc_id` int(11) NOT NULL,
  `rsv_resid` varchar(255) NOT NULL,
  `rsv_category` int(11) NOT NULL,
  `rsv_state` int(11) NOT NULL,
  `rsv_pending` int(11) NOT NULL,
  `rsv_joining` int(11) DEFAULT NULL,
  `shirakoproperties` longtext,
  PRIMARY KEY (`rsv_id`),
  UNIQUE KEY `rsv_slc_id` (`rsv_slc_id`,`rsv_resid`),
  KEY `state` (`rsv_state`),
  KEY `pending` (`rsv_pending`),
  KEY `joining` (`rsv_joining`),
  KEY `category` (`rsv_category`),
  CONSTRAINT `rsv_ibfk_1` FOREIGN KEY (`rsv_slc_id`) REFERENCES `Slices` (`slc_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=158 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Slices`
--

DROP TABLE IF EXISTS `Slices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Slices` (
  `slc_id` int(11) NOT NULL AUTO_INCREMENT,
  `slc_guid` varchar(255) NOT NULL,
  `slc_name` varchar(255) NOT NULL,
  `slc_type` varchar(255) NOT NULL,
  `slc_act_id` int(11) NOT NULL,
  `shirakoproperties` longtext,
  `slc_resource_type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`slc_id`),
  UNIQUE KEY `slc_guid` (`slc_guid`,`slc_act_id`),
  KEY `slc_ibfk_1` (`slc_act_id`),
  CONSTRAINT `slc_ibfk_1` FOREIGN KEY (`slc_act_id`) REFERENCES `Actors` (`act_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=926 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Units`
--

DROP TABLE IF EXISTS `Units`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Units` (
  `unt_id` int(11) NOT NULL AUTO_INCREMENT,
  `unt_uid` varchar(255) DEFAULT NULL,
  `unt_unt_id` int(11) DEFAULT NULL,
  `unt_act_id` int(11) DEFAULT NULL,
  `unt_slc_id` int(11) DEFAULT NULL,
  `unt_rsv_id` int(11) DEFAULT NULL,
  `unt_type` smallint(6) DEFAULT '1',
  `unt_state` int(11) NOT NULL,
  `shirakoproperties` longtext,
  PRIMARY KEY (`unt_id`),
  KEY `unt_ibfk_1` (`unt_slc_id`),
  KEY `unt_ibfk_2` (`unt_rsv_id`),
  KEY `unt_ibfk_3` (`unt_unt_id`),
  KEY `unt_ibfk_4` (`unt_act_id`),
  CONSTRAINT `unt_ibfk_1` FOREIGN KEY (`unt_slc_id`) REFERENCES `Slices` (`slc_id`) ON DELETE CASCADE,
  CONSTRAINT `unt_ibfk_2` FOREIGN KEY (`unt_rsv_id`) REFERENCES `Reservations` (`rsv_id`) ON DELETE CASCADE,
  CONSTRAINT `unt_ibfk_3` FOREIGN KEY (`unt_unt_id`) REFERENCES `Units` (`unt_id`) ON DELETE CASCADE,
  CONSTRAINT `unt_ibfk_4` FOREIGN KEY (`unt_act_id`) REFERENCES `Actors` (`act_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=166 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Users`
--

DROP TABLE IF EXISTS `Users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Users` (
  `usr_id` int(11) NOT NULL AUTO_INCREMENT,
  `usr_name` varchar(255) NOT NULL,
  `usr_password` varchar(255) DEFAULT '',
  `shirakoproperties` longtext,
  PRIMARY KEY (`usr_id`),
  UNIQUE KEY `usr_name` (`usr_name`)
) ENGINE=InnoDB AUTO_INCREMENT=184 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2014-01-11 14:57:54
