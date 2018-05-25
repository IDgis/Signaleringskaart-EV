# Signaleringskaart-EV

## Step 1: create docker-machine & network - once

- Create a docker-machine with ``docker-machine create ev-signalering-overijssel`` 
- Create a network called ev-ov with ``docker network create ev-ov`` 

## Step 2: add necessary files

- Go inside folder ``docker/database`` and create a folder called data. 
- Copy the files ``ev_lime_ov.sql`` and ``ev_services_ov.sql`` inside path ``docker/database/data`` 
- Copy the file ``deploy_ov.sh`` inside path ``docker`` 

## Step 3: install

- Run the ``deploy_ov.sh`` script. 
- Once the containers are up, enter the command ``docker logs -f deegree-ov``. If there are any errors while starting up, just restart the container to load the settings again using ``docker restart deegree-ov``.

## Step 4: setup database location

- Go to ``overijssel.ev-signaleringskaart.nl`` and you will enter a setup screen. 
- Change the configuration to ``Database type: PostgreSQL``, ``Database location: db-ov`` (the container name where the databases are), ``Database user && password: *****``, ``Database name: ev_lime_ov`` and leave the table prefix empty. 

## Step 5: import sjabloon

- Log in as admin at ``overijssel.ev-signaleringskaart.nl/admin``. 
- Go to Configuratie -> Sjablooneditor and choose Importeer gegevens. 
- Import the file ``ev-signalering-ov.zip``. 


## Change default settings

- Go to Configuratie -> Algemene instellingen. Here you can change the default settings like the email and language. 

## Add users

- Go to Configuratie -> Gebruikersbeheer. Here you can add users and change their permissions. 

## Import a new survey

- Go to Enquetes and click on 'Een enquete aanmaken'. Here you can import a .lss file as a new survey.
