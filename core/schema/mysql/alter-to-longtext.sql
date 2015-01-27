-- Script to modify all shirakoproperties to longtext (if not using new schema file)

alter table Actors modify shirakoproperties longtext;
alter table Clients modify shirakoproperties longtext;
alter table ConfigMappings  modify shirakoproperties longtext;
alter table Inventory  modify shirakoproperties longtext;
alter table ManagerObjects modify shirakoproperties longtext;
alter table Miscellaneous modify shirakoproperties longtext;
alter table Packages modify shirakoproperties longtext;
alter table Plugins modify shirakoproperties longtext;
alter table Proxies modify shirakoproperties longtext;
alter table Reservations modify shirakoproperties longtext;
alter table Slices modify shirakoproperties longtext;
alter table Units modify shirakoproperties longtext;
alter table Users modify shirakoproperties longtext;

