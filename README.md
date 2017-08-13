# Shopping list

Backend for a shopping list app. Written is Scala, based on Finagle HTTP services.


## Build and launch

```
sbt docker:publishLocal
docker-compose up
```

## API

For user authentication we rely on `X-Auth-Token` and `User-Id` headers.


### Lists

#### Get available lists

```
GET /api/lists
```

Return format:

```
[
    {
        "id": <backend list id, string>,
        "name": <list name, string>,
        "items": {
            "id": <item id, string>,
            "name": <item name, string>,
            "qty": <quantity description, string>,
            "checked": <boolean>
        }
    }
]
```

#### Add

```
POST /api/lists/<name>
```

Return format:

```
<backend list id, string>
```

#### Edit

```
PUT /api/lists/<backend list id>?name=<name>
```

#### Delete

```
DELETE /api/lists/<backend list id>
```

#### Sync

To sync changes made offline:

```
POST /api/lists
 
[
    {
        "id: <backend list id, optional string>,
        "name": <list name, string>,
        "deleted": <boolean>,
        "items": {
            "id": <backend item id, optional string>,
            "name": <item name, string>,
            "qty": <quantity description, string>,
            "checked": <boolean>,
            "deleted": <boolean>,
            "ts": <timestamp of the last change>,
        }
        "ts": <timestamp of the last change>
    }
}
```


### Items

#### Add

```
POST /api/lists/<list id>/items/<item name>?qty=<quantity description>
```

Return format:

```
<backend item id, string>
```

#### Edit

```
PUT /api/lists/<list id>/items/<backend item id>?name=<name>&qty=<quantity description>
```

#### Remove

```
DELETE /api/lists/<list id>/items/<backend item id>
```


### Invitations

A user can invite another user to her list via generated link. A link contains invite key — a 
random string generated for the particular list.

#### Get invitation link

```
GET /api/invites/<backend list id>
```

Return format:

```
<invitation link, string>
```

#### Accept invitation

```
GET /api/invites/<invite key>
```

On this request backend checks which list corresponds to the given key and grants a user 
permissions for that list. After receiving reply client should retrieve updated lists.


## Synchronization

As far as we don't need to pop up changes automatically when several users are acting on the same
 list at the same time, updates are done as pull requests. To update the state client should send
 `GET /api/lists`.
 
When coming online after making offline changes client should send `POST /api/lists` with a diff
 of updated elements. To track changes client can store timestamps of the last successful 
 synchronization and of the last change. Client should send the `POST` request if there is a gap
  between those timestamps.

Merging happens on backend. Every item in the diff has a timestamp. If client timestamp is 
bigger than server timestamp for the same element, change is applied.
After `POST /api/lists` client should send `GET /api/lists` to retrieve the merged state.
 
 
## Database

To store all the backend data we use MongoDB. We assume every user will have only a few 
shopping lists with up to a hundred items in every one, so the expected amount of data is not too 
big. 

Using a replica set and preferring different members for read and write operations increases 
data availability. 

### Collections

#### users

```
{
    "_id": <user id, ObjectId>
}
```

#### invites

```
{
    "_id": <invite id, ObjectId>,
    "uid": <user id, string>,
    "list": <list id, string>,
    "key": <invitation key, string> 
}
```

#### lists

```
{
    "_id": <backend list id, ObjectId>,
    "name": <list name, string>,
    "items": [ <item id, string> ],
    "users": [ <user id, string> ],
    "ts": <timestamp of the last change>,
}
```

#### items

```
{
    "_id": <backend item id, ObjectId>,
    "name": <name, string>,
    "qty": <quantity descriptor, string>,
    "checked": <boolean>,
    "ts": <timestamp of the last change>
}
```


## Changes in the database on the device

For synchronization we need new fields to be stored on the client.

1. For every item and every list add optional string field `backendId`.
2. Timestamp of the last successful sync or update operation.
3. For every item and every list keep a timestamp of their last edit.
4. For every item and every list add boolean field `deleted`. Set it to `true` instead of 
removing an item when performed delete operation and failed to sync.