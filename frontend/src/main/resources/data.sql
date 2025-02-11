insert into product (id, name, picture_url, price) values (1,'TV Set', 'http://placehold.it/200x100', 300) on conflict do nothing;
insert into product (id, name, picture_url, price) values (2,'Game Console', 'http://placehold.it/200x100', 200) on conflict do nothing;
insert into product (id, name, picture_url, price) values (3,'Sofa', 'http://placehold.it/200x100', 100) on conflict do nothing;
insert into product (id, name, picture_url, price) values (4,'Ice Cream', 'http://placehold.it/200x100', 5) on conflict do nothing;
insert into product (id, name, picture_url, price) values (5,'Beer', 'http://placehold.it/200x100', 3) on conflict do nothing;
insert into product (id, name, picture_url, price) values (6,'Phone', 'http://placehold.it/200x100', 500) on conflict do nothing;
insert into product (id, name, picture_url, price) values (7,'Watch', 'http://placehold.it/200x100', 30) on conflict do nothing;
insert into product (id, name, picture_url, price) values (8,'USB Cable', 'http://placehold.it/200x100',4) on conflict do nothing;
insert into product (id, name, picture_url, price) values (9,'USB-C Cable', 'http://placehold.it/200x100', 5) on conflict do nothing;
insert into product (id, name, picture_url, price) values (10,'Micro USB Cable', 'http://placehold.it/200x100', 3) on conflict do nothing;
insert into product (id, name, picture_url, price) values (11,'Lightning Cable', 'http://placehold.it/200x100', 9) on conflict do nothing;
insert into product (id, name, picture_url, price) values (12,'USB-C Adapter', 'http://placehold.it/200x100', 5) on conflict do nothing;

truncate order_product, orders;
