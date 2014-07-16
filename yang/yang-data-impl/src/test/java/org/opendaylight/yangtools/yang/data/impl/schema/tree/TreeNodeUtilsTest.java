package org.opendaylight.yangtools.yang.data.impl.schema.tree;

import com.google.common.base.Optional;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.spi.TreeNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.spi.TreeNodeFactory;
import org.opendaylight.yangtools.yang.data.api.schema.tree.spi.Version;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.Assert.*;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.*;


public class TreeNodeUtilsTest {
    private static final Logger LOG = LoggerFactory.getLogger(TreeNodeUtilsTest.class);

    private static final Short ONE_ID = 1;
    private static final Short TWO_ID = 2;
    private static final String TWO_ONE_NAME = "one";
    private static final String TWO_TWO_NAME = "two";

    private static final InstanceIdentifier OUTER_LIST_1_PATH = InstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
            .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, ONE_ID) //
            .build();

    private static final InstanceIdentifier OUTER_LIST_2_PATH = InstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
            .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, TWO_ID) //
            .build();

    private static final InstanceIdentifier TWO_TWO_PATH = InstanceIdentifier.builder(OUTER_LIST_2_PATH)
            .node(TestModel.INNER_LIST_QNAME) //
            .nodeWithKey(TestModel.INNER_LIST_QNAME, TestModel.NAME_QNAME, TWO_TWO_NAME) //
            .build();

    private static final MapEntryNode BAR_NODE = mapEntryBuilder(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, TWO_ID) //
            .withChild(mapNodeBuilder(TestModel.INNER_LIST_QNAME) //
                    .withChild(mapEntry(TestModel.INNER_LIST_QNAME, TestModel.NAME_QNAME, TWO_ONE_NAME)) //
                    .withChild(mapEntry(TestModel.INNER_LIST_QNAME, TestModel.NAME_QNAME, TWO_TWO_NAME)) //
                    .build()) //
            .build();

    private SchemaContext schemaContext;
    private RootModificationApplyOperation rootOper;

    @Before
    public void prepare() {
        schemaContext = TestModel.createTestContext();
        assertNotNull("Schema context must not be null.", schemaContext);
        rootOper = RootModificationApplyOperation.from(SchemaAwareApplyOperation.from(schemaContext));
    }

    public NormalizedNode<?, ?> createDocumentOne() {
        return ImmutableContainerNodeBuilder
                .create()
                .withNodeIdentifier(new InstanceIdentifier.NodeIdentifier(schemaContext.getQName()))
                .withChild(createTestContainer()).build();

    }

    private ContainerNode createTestContainer() {
        return ImmutableContainerNodeBuilder
                .create()
                .withNodeIdentifier(new InstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME))
                .withChild(
                        mapNodeBuilder(TestModel.OUTER_LIST_QNAME)
                                .withChild(mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, ONE_ID))
                                .withChild(BAR_NODE).build()).build();
    }

    private static <T> T assertPresentAndType(final Optional<?> potential, final Class<T> type) {
        assertNotNull(potential);
        assertTrue(potential.isPresent());
        assertTrue(type.isInstance(potential.get()));
        return type.cast(potential.get());
    }

    @Test
    public void findNodeTestNodeFound() {
        InMemoryDataTreeSnapshot inMemoryDataTreeSnapshot = new InMemoryDataTreeSnapshot(schemaContext,
                TreeNodeFactory.createTreeNode(createDocumentOne(), Version.initial()), rootOper);
        TreeNode rootNode = inMemoryDataTreeSnapshot.getRootNode();
        Optional<TreeNode> node = TreeNodeUtils.findNode(rootNode, OUTER_LIST_1_PATH);
        assertPresentAndType(node, TreeNode.class);
    }

    @Test
    public void findNodeTestNodeNotFound() {
        InMemoryDataTreeSnapshot inMemoryDataTreeSnapshot = new InMemoryDataTreeSnapshot(schemaContext,
                TreeNodeFactory.createTreeNode(createDocumentOne(), Version.initial()), rootOper);
        TreeNode rootNode = inMemoryDataTreeSnapshot.getRootNode();
        final InstanceIdentifier outerList1InvalidPath = InstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 3) //
                .build();
        Optional<TreeNode> node = TreeNodeUtils.findNode(rootNode, outerList1InvalidPath);
        Assert.assertFalse(node.isPresent());
    }

    @Test
    public void findNodeCheckedTestNodeFound() {
        InMemoryDataTreeSnapshot inMemoryDataTreeSnapshot = new InMemoryDataTreeSnapshot(schemaContext,
                TreeNodeFactory.createTreeNode(createDocumentOne(), Version.initial()), rootOper);
        TreeNode rootNode = inMemoryDataTreeSnapshot.getRootNode();
        TreeNode foundNode = null;
        try {
            foundNode = TreeNodeUtils.findNodeChecked(rootNode, OUTER_LIST_1_PATH);
        } catch (IllegalArgumentException e) {
            fail("Illegal argument exception was thrown and should not have been" + e.getMessage());
        }
        Assert.assertNotNull(foundNode);
    }

    @Test
    public void findNodeCheckedTestNodeNotFound() {
        InMemoryDataTreeSnapshot inMemoryDataTreeSnapshot = new InMemoryDataTreeSnapshot(schemaContext,
                TreeNodeFactory.createTreeNode(createDocumentOne(), Version.initial()), rootOper);
        TreeNode rootNode = inMemoryDataTreeSnapshot.getRootNode();
        final InstanceIdentifier outerList1InvalidPath = InstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 3) //
                .build();
        try {
            TreeNodeUtils.findNodeChecked(rootNode, outerList1InvalidPath);
            fail("Illegal argument exception should have been thrown");
        } catch (IllegalArgumentException e) {
            LOG.debug("Illegal argument exception was thrown as expected: '{}' - '{}'", e.getClass(), e.getMessage());
        }
    }

    @Test
    public void findClosestOrFirstMatchTestNodeExists() {
        InMemoryDataTreeSnapshot inMemoryDataTreeSnapshot = new InMemoryDataTreeSnapshot(schemaContext,
                TreeNodeFactory.createTreeNode(createDocumentOne(), Version.initial()), rootOper);
        TreeNode rootNode = inMemoryDataTreeSnapshot.getRootNode();
        Optional<TreeNode> expectedNode = TreeNodeUtils.findNode(rootNode, TWO_TWO_PATH);
        assertPresentAndType(expectedNode, TreeNode.class);
        Map.Entry<InstanceIdentifier, TreeNode> actualNode = TreeNodeUtils.findClosest(rootNode, TWO_TWO_PATH);
        Assert.assertEquals("Expected node and actual node are not the same", expectedNode.get(), actualNode.getValue());
    }

    @Test
    public void findClosestOrFirstMatchTestNodeDoesNotExist() {
        InMemoryDataTreeSnapshot inMemoryDataTreeSnapshot = new InMemoryDataTreeSnapshot(schemaContext,
                TreeNodeFactory.createTreeNode(createDocumentOne(), Version.initial()), rootOper);
        TreeNode rootNode = inMemoryDataTreeSnapshot.getRootNode();
        final InstanceIdentifier outerListInnerListPath = InstanceIdentifier.builder(OUTER_LIST_2_PATH)
                .node(TestModel.INNER_LIST_QNAME)
                .build();
        final InstanceIdentifier twoTwoInvalidPath = InstanceIdentifier.builder(OUTER_LIST_2_PATH)
                .node(TestModel.INNER_LIST_QNAME) //
                .nodeWithKey(TestModel.INNER_LIST_QNAME, TestModel.NAME_QNAME, "three") //
                .build();
        Optional<TreeNode> expectedNode = TreeNodeUtils.findNode(rootNode, outerListInnerListPath);
        assertPresentAndType(expectedNode, TreeNode.class);
        Map.Entry<InstanceIdentifier, TreeNode> actualNode = TreeNodeUtils.findClosest(rootNode, twoTwoInvalidPath);
        Assert.assertEquals("Expected node and actual node are not the same", expectedNode.get(), actualNode.getValue());
    }

    @Test
    public void getChildTestChildFound() {
        InMemoryDataTreeSnapshot inMemoryDataTreeSnapshot = new InMemoryDataTreeSnapshot(schemaContext,
                TreeNodeFactory.createTreeNode(createDocumentOne(), Version.initial()), rootOper);
        TreeNode rootNode = inMemoryDataTreeSnapshot.getRootNode();
        Optional<TreeNode> node = TreeNodeUtils.getChild(Optional.fromNullable(rootNode),
                TestModel.TEST_PATH.getLastPathArgument());
        assertPresentAndType(node, TreeNode.class);
    }

    @Test
    public void getChildTestChildNotFound() {
        InMemoryDataTreeSnapshot inMemoryDataTreeSnapshot = new InMemoryDataTreeSnapshot(schemaContext,
                TreeNodeFactory.createTreeNode(createDocumentOne(), Version.initial()), rootOper);
        TreeNode rootNode = inMemoryDataTreeSnapshot.getRootNode();
        Optional<TreeNode> node = TreeNodeUtils.getChild(Optional.fromNullable(rootNode),
                TestModel.OUTER_LIST_PATH.getLastPathArgument());
        Assert.assertFalse(node.isPresent());
    }
}
