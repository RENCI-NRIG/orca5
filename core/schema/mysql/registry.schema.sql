USE ActorRegistry;

DROP TABLE IF EXISTS `Actors`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
CREATE TABLE `Actors` (
  `act_id` int(11) NOT NULL auto_increment,
  `act_name` varchar(255) NOT NULL,
  `act_guid` varchar(255) NOT NULL,
  `act_type` int(11) NOT NULL,
  `act_desc` text,
  `act_soapaxis2url` text,
  `act_class` text,
  `act_mapper_class` text,
  `act_pubkey` text,
  `act_cert64` text,
  `act_abstract_rdf` text,
  `act_full_rdf` text,
  `act_allocatable_units` text,
  `act_production_deployment` text,
  `act_last_update` datetime,
  PRIMARY KEY  (`act_id`),
  UNIQUE KEY `act_guid` (`act_guid`)
) ENGINE=InnoDB AUTO_INCREMENT=175 DEFAULT CHARSET=utf8;
SET character_set_client = @saved_cs_client;
