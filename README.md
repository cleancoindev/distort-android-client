# DistoRt Client  <img src="images/icon.png" width="50" height="50" />

### About
This is the reference implementation for an Android client for DistoRt, a platform for providing anonymous and 
encrypted messaging between peers (designed by [JS Légaré](https://github.com/init-js)). 
Large anonymity groups are used to obscure the intended recipient of a message, 
and modern encryption schemes are used to ensure confidentiality.
The client allows for interacting with a [DistoRt homeserver](https://github.com/ryco117/distort-server) 
remotely through the server's REST API. Supports both HTTP and HTTPS homeserver authentication. 
User installed certificates are trusted during HTTPS requests, so that if users  create self-signed 
certificates for their homeserver, then the client will be able to accept the certificate 
if the user manually installs it to their Android device.


### Images
* #### Login
    <img src="images/login.png" width="300" height="533" />
* #### Messaging 
    <img src="images/messaging.png" width="300" height="533" />
* #### Notifications
    <img src="images/notifications.png" width="300" height="183" />
* #### Leave Group
    <img src="images/leave_group.png" width="300" height="533" />
* #### Display Account as QR Code
    <img src="images/display_qr.png" width="300" height="533" />
* #### Read Peer Accounts as QR Codes
    <img src="images/read_qr.png" width="300" height="533" />
