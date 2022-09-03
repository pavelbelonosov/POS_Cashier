--disable users with public role(all new users by default) to connect to dbs
revoke connect on database shoppos from public;
revoke connect on database postgres from public;

--disable superuser
alter user shoppos nosuperuser createdb createrole inherit login;

--crete role for application
create role client_role;
grant connect on database shoppos to client_role;
grant usage on schema shoppos to client_role;
alter default privileges in schema shoppos grant select,insert,update,delete on tables to client_role;
alter default privileges in schema shoppos grant all on sequences to client_role;

--specify new user with client role
create user client with encrypted password 'client166831';
grant client_role to client;