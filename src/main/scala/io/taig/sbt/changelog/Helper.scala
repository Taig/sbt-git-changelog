package io.taig.sbt.changelog

import cats.data.Xor
import cats.syntax.xor._
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.revwalk.RevWalk

import scala.collection.JavaConversions._
import scala.language.reflectiveCalls

object Helper {
    /**
     * A Range is either a SINCE..UNTIL combination, or only a start value
     * such as HEAD
     */
    type Range = ( AnyObjectId, AnyObjectId ) Xor AnyObjectId

    /**
     * Gets the most recent tags (one commit may have several tags) ignoring
     * the current HEAD
     */
    def recentTags( implicit g: Git ): List[String] = {
        try {
            val commits = g.log().call().iterator().toList.drop( 1 )
            val tags = g.tagList().call().toList

            commits
                .map { commit ⇒
                    tags
                        .filter( _.getObjectId == commit.getId )
                        .map( _.getName )
                        .filter( _.startsWith( "refs/tags/" ) )
                        .map( _.replace( "refs/tags/", "" ) )
                }
                .headOption
                .getOrElse( List.empty )
        } catch {
            case _: NoHeadException ⇒ List.empty
        }
    }

    /**
     * Gets a commit Iterator for all commits from the given range
     */
    def commits( range: Option[Range] )( implicit g: Git ): RevWalk = {
        val walk = new RevWalk( g.getRepository )

        range.getOrElse( HEAD.right ) match {
            case Xor.Left( ( since, until ) ) ⇒
                walk.markStart( walk.lookupCommit( until ) )
                walk.markUninteresting( walk.lookupCommit( since ) )
            case Xor.Right( start ) ⇒
                walk.markStart( walk.lookupCommit( start ) )
        }

        walk
    }

    def parseRange( range: String )( implicit g: Git ): Option[( String, String ) Xor String] = {
        range.split( "\\.\\." ) match {
            case Array( since, until ) ⇒ Some( ( since, until ).left )
            case Array( start )        ⇒ Some( start.right )
            case _                     ⇒ None
        }
    }

    def resolveDefaultRange( tags: List[String] )( implicit g: Git ): Range = {
        recentTags match {
            case head :: _ ⇒ ( resolve( head ), HEAD ).left
            case _         ⇒ HEAD.right
        }
    }

    def HEAD( implicit g: Git ) = resolve( "HEAD" )

    def resolve( id: String )( implicit g: Git ) = {
        g.getRepository.resolve( id )
    }

    def using[T <: { def close() }, U]( resource: T )( block: T ⇒ U ): U = {
        try {
            block( resource )
        } finally {
            if ( resource != null ) resource.close()
        }
    }
}