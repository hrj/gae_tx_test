# GAE transactions and unique key constraints

## Introduction
While I was learning about the concept of Transactions on the Google App Engine data store
I came across this [interesting blog][bp] post by Broc Seib. In that post Broc describes
a method of implementing a Unique Key constraint by using transactions. In a nutshell the idea is
to check for existing entities with the same property value before inserting / updating an entity with
the same property value.

However, towards the end of the post there was a limitation mentioned that was a little jarring. Quoting:

> Limitations: You can only have a single “unique constraint” on your entity.
> The pre-exists query is an ancestor query, which forces us to search by entity parameters rather than just lookup the entity by its key.
> Likewise, those entity parameters must correspond directly with the unique key of the entity.
> It is ultimately the key that is unique, and you can only have one of them.
> This is also why we end up with “duplicated data” in our entity, i.e. the seatId field is also used as the entityId separately.

I am a newbiew to GAE but I think this part of the post is wrong; my take:

1. The pre-exists check can be just a key lookup. Hence, there is no need to create another redundant parameter which is a copy of the key.
2. The pre-exists check can be any ancestor query as well. And this means you can have multiple unique constraints in the same entity group.

The only limitation I see is that transactions only work within an entity group. So the uniqueness check
is only effective within the entity group. Hence, you need to carefully plan out the ancestor path of an entity
when you need some properties to be unique.

## Code demo
When I posted the above concerns on Broc's blog, Broc suggeted that I try out my ideas in code. Hence this project. Along with test code that tries to mimic a large mob reserving a set of limited seats.

In the project are two servlets that respond to the same API. The API is described futher down. The two servlets paths are:
1. `/seatreservation/`  Uses the original code from Broc's blog post.
2. `/seatreservationkeybased/` Similar to above but uses a key lookup

## API

### POST {servletpath}/reserve
Parameters:

  * ownerName : String
  * seatId : String, should be one of "s1", "s2" ... "s500"

Output is JSON. Different output possibilities are:

  * `{result:"seat_reserved", ownerName:"moby", seatId:"s10", retries:4}`
  * `{result:"seat_taken"}`
  * `{result:"illegal_seat_request"}`

### GET {servletpath}
Output: JSON array containing all reservations. Eg:
`[ {ownerName:"moby", seatId:"10A"}, {...}, .... ]`

### POST {servletpath}/clearAll
This will clear all reservations. Can be called before starting a stress test.

  [bp]:http://blog.broc.seib.net/2011/06/unique-constraint-in-appengine.html
