# Part 1: Problem in ShardedCounter
There is [an article][sca] in the Google App Engine docs about optimally implementing counters. The proposed
strategy, called sharding, is to use multiple counter entities rather than a single entity so that contention is reduced.

However, I found a major problems with this article:

The sample code given doesn't handle the `ConcurrentModificationException`, which is a major omission I think.
In my tests, under high load, 

   * about 12% of the counter increments are missed when shard count is 2 and
   * about 6% are missed when shard count is 5!

The solution is simple; catch the `ConcurrentModificationException` and retry.

  [sca]:http://code.google.com/appengine/articles/sharding_counters.html

## Code demo
There is a servlet in this project called CounterServlet which uses the ShardedCounter code from the
article, catches the `ConcurrentModificationException` and records how many times it happened.

There is a python script `tests/testCounter.py` to test this servlet.

The API is defined below.

### POST /increment
Causes the counter within the servlet to increment

### POST /clearAll
Clears all counter servlet data. Useful when beginning a test.

### GET /
Returns the current count, the number of shards and the number of missed exceptions.

# Part 2: GAE transactions and unique property constraints
While I was learning about the concept of Transactions on the Google App Engine data store
I came across this [interesting blog][bp] post by Broc Seib. In that post Broc describes
a method of implementing a Unique property constraint by using transactions. In a nutshell the idea is
to check for existing entities with the same property value in a transaction before inserting / updating an entity with
the same property value. This was a nice post and I learnt several things from it.

However, towards the end of the post there was a limitation mentioned that was a little jarring. Quoting:

> Limitations: You can only have a single “unique constraint” on your entity.
> The pre-exists query is an ancestor query, which forces us to search by entity parameters rather than just lookup the entity by its key.
> Likewise, those entity parameters must correspond directly with the unique key of the entity.
> It is ultimately the key that is unique, and you can only have one of them.
> This is also why we end up with “duplicated data” in our entity, i.e. the seatId field is also used as the entityId separately.

I am a newbiew to GAE but I think this part of the post is wrong; here's my take:

1. The pre-exists check can be just a key lookup, if used within a transaction.
Hence, there is no need to create a redundant property containing a copy of the key.
And when there is no property to be queried there is no need for an ancestor for your Entity.
2. The pre-exists check can be any ancestor query as well.
And this means you can have multiple unique constraints in the same entity group.

The only limitation I see is that transactions work only within an entity group (\*). So the uniqueness check
is only effective within the entity group. You need to carefully plan out the ancestor path of an entity
when you need some properties to be unique. In case of root entities, expect lots of collisions.

\* There are cross-group transactions as well but beyond the scope of this article.

# Code demo
When I posted the above concerns on Broc's blog, Broc suggeted that I try out my ideas in code. Hence this project. Along with test code that tries to mimic a large mob reserving a set of limited seats.

In the project are two servlets that respond to the same API. The API is described futher down. The two servlets paths are:

1. `/seatreservation/`  Uses the original code from Broc's blog post.
2. `/seatreservationkeybased/` Similar to the one above but uses a key lookup
3. `/seatreservation_badkey/` Similar to the one above but has an incorrect key lookup - not mentionting the transaction.

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
`[ {ownerName:"moby", seatId:"s10"}, {...}, .... ]`

### POST {servletpath}/clearAll
This will clear all reservations. Can be called before starting a stress test.

  [bp]:http://blog.broc.seib.net/2011/06/unique-constraint-in-appengine.html
