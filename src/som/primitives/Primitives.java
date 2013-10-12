/**
 * Copyright (c) 2013 Stefan Marr,   stefan.marr@vub.ac.be
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package som.primitives;

import som.compiler.MethodGenerationContext;
import som.interpreter.Primitive;
import som.interpreter.nodes.ArgumentEvaluationNode;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.PrimitiveNode;
import som.interpreter.nodes.VariableNode.SelfReadNode;
import som.interpreter.nodes.VariableNode.VariableReadNode;
import som.vm.Universe;
import som.vmobjects.SClass;
import som.vmobjects.SMethod;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.FrameSlot;

public abstract class Primitives {

  protected final Universe universe;

  public Primitives(final Universe universe) {
    this.universe = universe;
  }

  public final void installPrimitivesIn(final SClass value) {
    // Save a reference to the holder class
    holder = value;

    // Install the primitives from this primitives class
    installPrimitives();
  }

  public abstract void installPrimitives();

  public static SMethod constructPrimitive(final String selector,
      final NodeFactory<? extends PrimitiveNode> nodeFactory, final Universe universe) {
    SSymbol signature = universe.symbolFor(selector);
    int numArgs = signature.getNumberOfSignatureArguments() - 1; // we take care of self seperately


    MethodGenerationContext mgen = new MethodGenerationContext();
    ExpressionNode[] args = new ExpressionNode[numArgs];
    FrameSlot[] argSlots  = new FrameSlot[numArgs];
    for (int i = 0; i < numArgs; i++) {
      argSlots[i] = mgen.addArgument("primArg" + i);
      args[i] = new VariableReadNode(argSlots[i], 0);
    }

    ArgumentEvaluationNode argEvalNode = new ArgumentEvaluationNode(args);

    PrimitiveNode primNode = nodeFactory.createNode(signature, universe,
          new SelfReadNode(mgen.getSelfSlot(), 0), argEvalNode);

    Primitive primMethodNode = new Primitive(primNode, mgen.getSelfSlot(),
        argSlots, mgen.getFrameDescriptor());
    SMethod prim = universe.newMethod(signature, primMethodNode,
        mgen.getFrameDescriptor(), true);

    return prim;
  }

  protected void installInstancePrimitive(final String selector,
      final NodeFactory<? extends PrimitiveNode> nodeFactory) {
    SMethod prim = constructPrimitive(selector, nodeFactory, universe);
    // Install the given primitive as an instance primitive in the holder class
    holder.addInstancePrimitive(prim);
  }

  protected void installClassPrimitive(final String selector,
      final NodeFactory<? extends PrimitiveNode> nodeFactory) {
    SMethod prim = constructPrimitive(selector, nodeFactory, universe);

    // Install the given primitive as an instance primitive in the class of
    // the holder class
    holder.getSOMClass().addInstancePrimitive(prim);
  }

  private SClass holder;

  public static SMethod getEmptyPrimitive(final String selector,
      final Universe universe) {
    return constructPrimitive(selector, EmptyPrimFactory.getInstance(), universe);
  }
}
