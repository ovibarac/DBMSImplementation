Run server: `Server`

Run client: `Client`

Test commands:

    create database db;
    use db;
    CREATE TABLE Orders (orderId INT, userId INT, PRIMARY KEY (orderId), FOREIGN KEY (userId) REFERENCES Users(id));
    CREATE TABLE disciplines (DiscID varchar(5) PRIMARY KEY, DName varchar(20), CreditNr int);
    drop table Orders
    DROP DATABASE db;
    CREATE INDEX idx_orderid ON Orders (orderId)
    CREATE UNIQUE INDEX idx_disc ON disciplines (DiscID, DName)
    INSERT INTO Orders VALUES (19,10);
    INSERT INTO Orders VALUES (3,8);
    INSERT INTO Orders VALUES (1,1);
    DELETE FROM Orders WHERE id = 1;

    INSERT INTO Users VALUES (4,muwu,puwu);
    CREATE TABLE Test (testId INT, uId INT, PRIMARY KEY (testId), FOREIGN KEY (uId) REFERENCES Users(uId));
    CREATE UNIQUE INDEX idx_usrn ON Users (username)
    CREATE INDEX idx_usrn ON Users (username)
    DELETE FROM Users WHERE id = 2;

    INSERT INTO Orders VALUES (3,3);
    INSERT INTO Orders VALUES (3,7);
    INSERT INTO Users VALUES (4,muwu,great);
    INSERT INTO Users VALUES (5,adk,kda);
    INSERT INTO Orders VALUES (3,5);

Result in `db_schema.xml`