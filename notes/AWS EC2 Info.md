
My home IP (dynamic): 76.146.73.81

Instance address: ec2-54-189-164-174.us-west-2.compute.amazonaws.com

ssh into EC2 instance command: ```
```
ssh -i CS6650.pem ec2-user@instance-address
```

scp into tomcat webapps dir:
```
scp -i CS6650.pem <path/to/war> ec2-user@instance-address:/usr/share/tomcat/webapps
```

location of tomcat's `server.xml` on EC2 instance:
```
/usr/share/apache-tomcat-9.0.63/conf/server.xml
```

Tomcat admin creds:
- username: admin
- password: admin666

EC2 instance `systemctl` commands:
- `sudo systemctl daemon-reload`
- `sudo systemctl restart tomcat`