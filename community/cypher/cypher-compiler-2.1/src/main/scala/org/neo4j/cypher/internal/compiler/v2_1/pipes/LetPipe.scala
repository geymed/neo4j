/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.v2_1.pipes

import org.neo4j.cypher.internal.compiler.v2_1.commands.expressions.Expression
import org.neo4j.cypher.internal.compiler.v2_1.symbols.SymbolTable
import org.neo4j.cypher.internal.compiler.v2_1.ExecutionContext
import org.neo4j.cypher.internal.compiler.v2_1.data.SimpleVal

case class LetPipe(source: Pipe, expressions: Map[String, Expression])
                  (implicit pipeMonitor: PipeMonitor) extends PipeWithSource(source, pipeMonitor) {

  val symbols: SymbolTable = {
    val newIdentifiers = expressions.map {
      case (name, expression) => name -> expression.getType(source.symbols)
    }

    source.symbols.add(newIdentifiers)
  }

  protected def internalCreateResults(input: Iterator[ExecutionContext], state: QueryState) =
    input.map { original =>
      expressions.foldLeft(original) {
        case (context, (name, expression)) =>  context += name -> expression(original)(state)
    }
  }

  override def executionPlanDescription =
    source.executionPlanDescription
      .andThen(this, "Let",
        "symKeys" -> SimpleVal.fromIterable(source.symbols.keys),
        "exprKeys" -> SimpleVal.fromIterable(expressions.keys))
}
