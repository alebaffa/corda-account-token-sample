# Token To Account Friend
In this Cordapp, you will be able to create and issue fungible tokens to an account. You can also move tokens from one account to another (in the same hosting node).
 
 ## Running the applications 
 ```
 ./gradlew deployNodes
 ./build/nodes/runnodes
 ```
 
## Running in terminal: 
Go to the operator node and perform the following steps:

1. create two accounts:

```shell
>>> flow start CreateNewAccountFlow accountName: Sally

 ✓ Starting
▶︎ Done
Flow completed with result: Sally account has been created

>>> flow start CreateNewAccountFlow accountName: Bob

 ✓ Starting
▶︎ Done
Flow completed with result: Bob account has been created
```
2. create a state:

```
 flow start CreateMyToken myEmail: 1@gmail.com, recipients: 2@gmail.com, msg: Corda Number 1! 
 ```
 then record the returned uuid
 ```
 flow start IssueToken uuid: xxx-xxxx-xxxx-xxxx-xx
 ```
 record the message returned, TokenId and storage node.
 
 Go to that storage node terminal: 
 ```
 flow start QueryToken uuid: xxx-xxxx-xxxx-xxxx-xx, recipientEmai: 2@gmail.com
 ```
 
You should discover the message that was attached in the token. 

## Runing in webapp
Open a new window and run the blow code for token issuance
```
./gradlew runOperatoreServer
```
To retrieve the token, because most people will run the app locally, by default I have the gradle task to start only one storage node's web server. 
```
./gradlew runUSWest1Server
```
After both servers started, go to localhost:10050 to issue a token and localhost:10053 to experience the retrieve. (The reason it is two different site is that communiticating among multiple local server is prohibit by CORS policy. In production environment, we do not need to go to a different site for retrieve.)





