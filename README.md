# Case study: The digital transformation of Santa's logistical nightmare
By Bones, Dog and Megachu.

<img height=150px src="https://user-images.githubusercontent.com/3103/207422703-66e0f264-f8e2-4ae2-91d4-6bd0f3cc20be.png" alt="Megachu">

# Introduction

This is the first post in a series walking through an example of a microservice with a Kafka integration using the Typelevel stack. We have chosen to showcase a simplified version of Santa's Ledger.  
Unbeknownst to most, Santa's systems are actually supported by cats (the ones that go meow). The work here was kindly translated into English by our keepers.

## Summary

This series of posts is a report on our experience building and running Santa's Ledger, a well known, reliable system of gifting.
One component of this report is a toy version of the real system designed for didactic purposes. This toy repo has representative examples of:

      * Using [IO](https://typelevel.org/cats-effect/docs/2.x/datatypes/io) rather than a finally tagless approach because we thought it might be easier to read for people not used to more functional approaches. Feedback on this most welcome    
      * Consuming messages off of [Kafka](https://kafka.apache.org)
      * Serializing and deserializing messages using serdes defined with [Vulcan](https://fd4s.github.io/vulcan/)
      * Maintaining concurrency-safe state using [Ref](https://typelevel.org/cats-effect/docs/std/ref) from the [cats-effect](https://typelevel.org/cats-effect/) standard lib
      * Routing using [HTTP4s](https://http4s.org)        
      * Writing tests using [munit](https://scalameta.org/munit/)
      * Using docker containers for integration test dependencies with [test-containers](https://github.com/testcontainers/testcontainers-scala)


To keep the toy project simple and to the point, there are a series of decisions and assumptions we have made. Please see #Decisions & Assumptions for details.

## Motivation

Some of our keeper's friends, who are coming from Akka and Alpakka, asked us about how to integrate with Kafka using the Typelevel stack.
We decided an example would be a great place to start.

We looked but in our (not overly extensive) search we were unable to find a recent and succinct example of the above and set off to building one!

More importantly, it's 🎄*Christmas-time* 🎄 and 🎅 *Santa* 🎅 needed a digital transformation.

## Requirements

With this example we aim to demonstrate the consumption of kafka messages, maintenance of local state and running an HTTP server using the Typelevel stack.
The requirements are as follows:

      * Santa's Address Book should process and store information about the latest address a kid lives at.
      * Santa's Ledger should process information about a kid's behaviour and keep track of what present they should receive.
      * Santa must be able to query the service for a kid's address and the present they should get. (a.k.a. A consignment)
      * Santa must be able to query the service for all consignments destined for an address.
          * This is to avoid costly repeat trips to a household. (Rudolph gets a bit tired...)



## Decisions & Assumptions

* You have some familiarity with Scala.
* Last names have a 1-to-1 mapping with address and full names are unique (everyone is very original in this world)
* Addresses are strings, because Santa and their team are very consistent with addressing and we didn't want to model this.
* You already know Kafka and how it works. When we had to make choices about what settings to use we chose whatever made the example easiest to understand.
* You already know how to write tests. We are not aiming to demonstrate good testing practices.

## What's next?
We'll dive deeper into some of the technologies used over a series of blog posts throughout the festive period. If you have any preference in order, please let us know.


This blog post was supervised by [Andrea Magnorsky](http://www.roundcrisis.com/) and [Elias Court](http://k1nd.ltd)

