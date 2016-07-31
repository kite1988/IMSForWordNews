Alternative Translation Backend for WordNews
=========

Based on IMS


Only the /show endpoint is implemented in this alternative backend. The parameters must be send as POST parameters.
This endpoint does not provide the quiz options or updates the user's learning history, i.e. the implemented here is "read-only".
The main reason is that the code here and database are not synced (there is no record of database migrations/evoluations in this code),
everytime the database schema has an update, the code here must be manually modified to support the new change. Therefore, 
it should be relatively important to quickly decide which backend should be the main backend, otherwise every new feature/schema change
implemented must be re-implemented in all backends. 

Setting Up
=========
This project is based on the Play Framework.

`activator` should be your PATH. Download Play framework at https://www.playframework.com/download. 
Play 2.4.3 and Activator version 1.3.7 was used to develop this alternative backend. 
To start running locally, run `activator run`. 

`conf/application.conf` specifies the database. `udb.default.url=${?DATABASE_URL}` references the DATABASE_URL environmental variable in Heroku.
For local development, comment out `udb.default.url=${?DATABASE_URL}` and 
configure `db.default.url="postgres://postgres:password@localhost:5432/postgres"` instead.

