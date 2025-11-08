package com.sla.dsa;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete Red-Black Tree implementation with all standard operations.
 * Provides O(log N) insertion, deletion, and search operations.
 * Provides O(log N + k) range query operations where k is the number of results.
 * 
 * Red-Black Tree Properties:
 * 1. Every node is either red or black
 * 2. Root is always black
 * 3. All leaves (null) are black
 * 4. Red nodes cannot have red children
 * 5. All paths from root to leaves have the same number of black nodes
 */
public class SelfBalancingSearchTree<T extends Comparable<T>> {
    
    private enum NodeColor { RED, BLACK }
    
    private static class TreeNode<T extends Comparable<T>> {
        T key;
        TreeNode<T> parent;
        TreeNode<T> left;
        TreeNode<T> right;
        NodeColor color;
        
        TreeNode(T key) {
            this.key = key;
            this.color = NodeColor.RED;
            this.left = null;
            this.right = null;
            this.parent = null;
        }
    }
    
    private TreeNode<T> root;
    private int size;
    
    public SelfBalancingSearchTree() {
        this.root = null;
        this.size = 0;
    }
    
    
    public void insert(T key) {
        TreeNode<T> newNode = new TreeNode<>(key);
        
        if (root == null) {
            root = newNode;
            root.color = NodeColor.BLACK;
            size++;
            return;
        }
        
        TreeNode<T> current = root;
        TreeNode<T> parent = null;
        
        while (current != null) {
            parent = current;
            if (key.compareTo(current.key) < 0) {
                current = current.left;
            } else {
                current = current.right;
            }
        }
        
        newNode.parent = parent;
        if (key.compareTo(parent.key) < 0) {
            parent.left = newNode;
        } else {
            parent.right = newNode;
        }
        
        size++;
        fixInsert(newNode);
    }
    
    /**
     * Delete a node from the tree.
     * Time complexity: O(log N)
     */
    public boolean delete(T key) {
        TreeNode<T> nodeToDelete = searchNode(root, key);
        if (nodeToDelete == null) {
            return false;
        }
        
        deleteNode(nodeToDelete);
        size--;
        return true;
    }
    
    private void deleteNode(TreeNode<T> node) {
        TreeNode<T> replacement;
        TreeNode<T> child;
        TreeNode<T> fixNode;
        TreeNode<T> fixParent;
        boolean fixIsLeft;
        NodeColor originalColor = node.color;
        
        if (node.left == null) {
            child = node.right;
            fixNode = child;
            fixParent = node.parent;
            fixIsLeft = (fixParent != null && node == fixParent.left);
            transplant(node, node.right);
            if (originalColor == NodeColor.BLACK) {
                if (fixNode != null) {
                    fixDelete(fixNode);
                } else if (fixParent != null) {
                    fixDeleteWithNullChild(fixParent, fixIsLeft);
                }
            }
        } else if (node.right == null) {
            child = node.left;
            fixNode = child;
            fixParent = node.parent;
            fixIsLeft = (fixParent != null && node == fixParent.left);
            transplant(node, node.left);
            if (originalColor == NodeColor.BLACK) {
                if (fixNode != null) {
                    fixDelete(fixNode);
                } else if (fixParent != null) {
                    fixDeleteWithNullChild(fixParent, fixIsLeft);
                }
            }
        } else {
            replacement = findMin(node.right);
            originalColor = replacement.color;
            child = replacement.right;
            fixParent = replacement.parent;
            fixIsLeft = (replacement.parent != null && replacement == replacement.parent.left);
            
            if (replacement.parent == node) {
                if (child != null) child.parent = replacement;
                fixParent = replacement;
                fixIsLeft = false;
            } else {
                transplant(replacement, replacement.right);
                replacement.right = node.right;
                if (replacement.right != null) {
                    replacement.right.parent = replacement;
                }
            }
            
            transplant(node, replacement);
            replacement.left = node.left;
            if (replacement.left != null) {
                replacement.left.parent = replacement;
            }
            replacement.color = node.color;
            
            if (originalColor == NodeColor.BLACK) {
                if (child != null) {
                    fixDelete(child);
                } else if (fixParent != null) {
                    fixDeleteWithNullChild(fixParent, fixIsLeft);
                }
            }
        }
    }
    
    private void fixDeleteWithNullChild(TreeNode<T> parent, boolean childWasLeft) {
        TreeNode<T> sibling;
        
        while (parent != null) {
            if (childWasLeft) {
                sibling = parent.right;
                
                if (getColor(sibling) == NodeColor.RED) {
                    sibling.color = NodeColor.BLACK;
                    parent.color = NodeColor.RED;
                    rotateLeft(parent);
                    sibling = parent.right;
                }
                
                if (sibling == null) {
                    if (parent == root || getColor(parent) == NodeColor.RED) {
                        parent.color = NodeColor.BLACK;
                        return;
                    }
                    childWasLeft = (parent.parent != null && parent == parent.parent.left);
                    parent = parent.parent;
                } else if (getColor(sibling.left) == NodeColor.BLACK && 
                    getColor(sibling.right) == NodeColor.BLACK) {
                    sibling.color = NodeColor.RED;
                    if (getColor(parent) == NodeColor.RED) {
                        parent.color = NodeColor.BLACK;
                        return;
                    }
                    if (parent == root) {
                        return;
                    }
                    childWasLeft = (parent.parent != null && parent == parent.parent.left);
                    parent = parent.parent;
                } else {
                    if (getColor(sibling.right) == NodeColor.BLACK) {
                        if (sibling.left != null) sibling.left.color = NodeColor.BLACK;
                        sibling.color = NodeColor.RED;
                        rotateRight(sibling);
                        sibling = parent.right;
                    }
                    if (sibling != null) {
                        sibling.color = parent.color;
                        parent.color = NodeColor.BLACK;
                        if (sibling.right != null) sibling.right.color = NodeColor.BLACK;
                        rotateLeft(parent);
                    }
                    return;
                }
            } else {
                sibling = parent.left;
                
                if (getColor(sibling) == NodeColor.RED) {
                    sibling.color = NodeColor.BLACK;
                    parent.color = NodeColor.RED;
                    rotateRight(parent);
                    sibling = parent.left;
                }
                
                if (sibling == null) {
                    if (parent == root || getColor(parent) == NodeColor.RED) {
                        parent.color = NodeColor.BLACK;
                        return;
                    }
                    childWasLeft = (parent.parent != null && parent == parent.parent.left);
                    parent = parent.parent;
                } else if (getColor(sibling.right) == NodeColor.BLACK && 
                    getColor(sibling.left) == NodeColor.BLACK) {
                    sibling.color = NodeColor.RED;
                    if (getColor(parent) == NodeColor.RED) {
                        parent.color = NodeColor.BLACK;
                        return;
                    }
                    if (parent == root) {
                        return;
                    }
                    childWasLeft = (parent.parent != null && parent == parent.parent.left);
                    parent = parent.parent;
                } else {
                    if (getColor(sibling.left) == NodeColor.BLACK) {
                        if (sibling.right != null) sibling.right.color = NodeColor.BLACK;
                        sibling.color = NodeColor.RED;
                        rotateLeft(sibling);
                        sibling = parent.left;
                    }
                    if (sibling != null) {
                        sibling.color = parent.color;
                        parent.color = NodeColor.BLACK;
                        if (sibling.left != null) sibling.left.color = NodeColor.BLACK;
                        rotateRight(parent);
                    }
                    return;
                }
            }
        }
    }
    
    private void transplant(TreeNode<T> oldNode, TreeNode<T> newNode) {
        if (oldNode.parent == null) {
            root = newNode;
        } else if (oldNode == oldNode.parent.left) {
            oldNode.parent.left = newNode;
        } else {
            oldNode.parent.right = newNode;
        }
        if (newNode != null) {
            newNode.parent = oldNode.parent;
        }
    }
    
    private void fixDelete(TreeNode<T> node) {
        while (node != root && getColor(node) == NodeColor.BLACK) {
            if (node == node.parent.left) {
                TreeNode<T> sibling = node.parent.right;
                
                if (getColor(sibling) == NodeColor.RED) {
                    sibling.color = NodeColor.BLACK;
                    node.parent.color = NodeColor.RED;
                    rotateLeft(node.parent);
                    sibling = node.parent.right;
                }
                
                if (sibling == null) {
                    node = node.parent;
                } else if (getColor(sibling.left) == NodeColor.BLACK && 
                    getColor(sibling.right) == NodeColor.BLACK) {
                    sibling.color = NodeColor.RED;
                    node = node.parent;
                } else {
                    if (getColor(sibling.right) == NodeColor.BLACK) {
                        if (sibling.left != null) sibling.left.color = NodeColor.BLACK;
                        sibling.color = NodeColor.RED;
                        rotateRight(sibling);
                        sibling = node.parent.right;
                    }
                    if (sibling != null) {
                        sibling.color = node.parent.color;
                        node.parent.color = NodeColor.BLACK;
                        if (sibling.right != null) sibling.right.color = NodeColor.BLACK;
                        rotateLeft(node.parent);
                    }
                    node = root;
                }
            } else {
                TreeNode<T> sibling = node.parent.left;
                
                if (getColor(sibling) == NodeColor.RED) {
                    sibling.color = NodeColor.BLACK;
                    node.parent.color = NodeColor.RED;
                    rotateRight(node.parent);
                    sibling = node.parent.left;
                }
                
                if (sibling == null) {
                    node = node.parent;
                } else if (getColor(sibling.right) == NodeColor.BLACK && 
                    getColor(sibling.left) == NodeColor.BLACK) {
                    sibling.color = NodeColor.RED;
                    node = node.parent;
                } else {
                    if (getColor(sibling.left) == NodeColor.BLACK) {
                        if (sibling.right != null) sibling.right.color = NodeColor.BLACK;
                        sibling.color = NodeColor.RED;
                        rotateLeft(sibling);
                        sibling = node.parent.left;
                    }
                    if (sibling != null) {
                        sibling.color = node.parent.color;
                        node.parent.color = NodeColor.BLACK;
                        if (sibling.left != null) sibling.left.color = NodeColor.BLACK;
                        rotateRight(node.parent);
                    }
                    node = root;
                }
            }
        }
        if (node != null) node.color = NodeColor.BLACK;
    }
    
    private NodeColor getColor(TreeNode<T> node) {
        return node == null ? NodeColor.BLACK : node.color;
    }
    
    /**
     * Search for an element in the tree.
     * Time complexity: O(log N)
     */
    public T search(T key) {
        TreeNode<T> node = searchNode(root, key);
        return node != null ? node.key : null;
    }
    
    /**
     * Helper method to search for a node.
     */
    private TreeNode<T> searchNode(TreeNode<T> node, T key) {
        if (node == null) {
            return null;
        }
        
        int comparison = key.compareTo(node.key);
        if (comparison == 0) {
            return node;
        } else if (comparison < 0) {
            return searchNode(node.left, key);
        } else {
            return searchNode(node.right, key);
        }
    }
    
    /**
     * Find minimum element in tree.
     * Time complexity: O(log N)
     */
    public T findMinimum() {
        if (root == null) return null;
        return findMin(root).key;
    }
    
    private TreeNode<T> findMin(TreeNode<T> node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }
    
    /**
     * Find maximum element in tree.
     * Time complexity: O(log N)
     */
    public T findMaximum() {
        if (root == null) return null;
        return findMax(root).key;
    }
    
    private TreeNode<T> findMax(TreeNode<T> node) {
        while (node.right != null) {
            node = node.right;
        }
        return node;
    }
    
    /**
     * Find predecessor of a given key.
     * Time complexity: O(log N)
     */
    public T findPredecessor(T key) {
        TreeNode<T> node = searchNode(root, key);
        if (node == null) return null;
        
        if (node.left != null) {
            return findMax(node.left).key;
        }
        
        TreeNode<T> parent = node.parent;
        while (parent != null && node == parent.left) {
            node = parent;
            parent = parent.parent;
        }
        return parent != null ? parent.key : null;
    }
    
    /**
     * Find successor of a given key.
     * Time complexity: O(log N)
     */
    public T findSuccessor(T key) {
        TreeNode<T> node = searchNode(root, key);
        if (node == null) return null;
        
        if (node.right != null) {
            return findMin(node.right).key;
        }
        
        TreeNode<T> parent = node.parent;
        while (parent != null && node == parent.right) {
            node = parent;
            parent = parent.parent;
        }
        return parent != null ? parent.key : null;
    }
    
    /**
     * Range query: Find all elements between startKey and endKey (inclusive).
     * Time complexity: O(log N + k) where k is the number of results
     */
    public List<T> rangeQuery(T startKey, T endKey) {
        List<T> result = new ArrayList<>();
        rangeQueryHelper(root, startKey, endKey, result);
        return result;
    }
    
    /**
     * Helper method for range query using in-order traversal.
     */
    private void rangeQueryHelper(TreeNode<T> node, T startKey, T endKey, List<T> result) {
        if (node == null) {
            return;
        }
        
        if (startKey.compareTo(node.key) < 0) {
            rangeQueryHelper(node.left, startKey, endKey, result);
        }
        
        if (startKey.compareTo(node.key) <= 0 && endKey.compareTo(node.key) >= 0) {
            result.add(node.key);
        }
        
        if (endKey.compareTo(node.key) > 0) {
            rangeQueryHelper(node.right, startKey, endKey, result);
        }
    }
    
    /**
     * Get all elements in sorted order via in-order traversal.
     * Time complexity: O(N)
     */
    public List<T> inOrderTraversal() {
        List<T> result = new ArrayList<>();
        inOrderHelper(root, result);
        return result;
    }
    
    private void inOrderHelper(TreeNode<T> node, List<T> result) {
        if (node != null) {
            inOrderHelper(node.left, result);
            result.add(node.key);
            inOrderHelper(node.right, result);
        }
    }
    
    /**
     * Get tree size.
     */
    public int size() {
        return size;
    }
    
    /**
     * Check if tree is empty.
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * Validate Red-Black Tree properties.
     * Returns true if tree satisfies all RBT properties.
     */
    public boolean validateTree() {
        if (root == null) return true;
        
        // Property 2: Root must be black
        if (root.color != NodeColor.BLACK) {
            return false;
        }
        
        // Check other properties and black height
        return validateNode(root) != -1;
    }
    
    private int validateNode(TreeNode<T> node) {
        if (node == null) {
            return 1; // Null nodes are black
        }
        
        // Property 4: Red node cannot have red children
        if (node.color == NodeColor.RED) {
            if (getColor(node.left) == NodeColor.RED || getColor(node.right) == NodeColor.RED) {
                return -1;
            }
        }
        
        // Property 5: All paths must have same black height
        int leftBlackHeight = validateNode(node.left);
        int rightBlackHeight = validateNode(node.right);
        
        if (leftBlackHeight == -1 || rightBlackHeight == -1 || leftBlackHeight != rightBlackHeight) {
            return -1;
        }
        
        return leftBlackHeight + (node.color == NodeColor.BLACK ? 1 : 0);
    }
    
    /**
     * Get black height of the tree.
     */
    public int getBlackHeight() {
        return calculateBlackHeight(root);
    }
    
    private int calculateBlackHeight(TreeNode<T> node) {
        if (node == null) return 0;
        
        int height = calculateBlackHeight(node.left);
        if (node.color == NodeColor.BLACK) {
            height++;
        }
        return height;
    }
    
    /**
     * Get tree height.
     */
    public int getHeight() {
        return calculateHeight(root);
    }
    
    private int calculateHeight(TreeNode<T> node) {
        if (node == null) return 0;
        return 1 + Math.max(calculateHeight(node.left), calculateHeight(node.right));
    }
    
    /**
     * Fix Red-Black Tree properties after insertion.
     */
    private void fixInsert(TreeNode<T> node) {
        while (node.parent != null && node.parent.color == NodeColor.RED) {
            TreeNode<T> parentNode = node.parent;
            TreeNode<T> grandparentNode = parentNode.parent;
            
            if (parentNode == grandparentNode.left) {
                TreeNode<T> uncleNode = grandparentNode.right;
                
                if (uncleNode != null && uncleNode.color == NodeColor.RED) {
                    parentNode.color = NodeColor.BLACK;
                    uncleNode.color = NodeColor.BLACK;
                    grandparentNode.color = NodeColor.RED;
                    node = grandparentNode;
                } else {
                    if (node == parentNode.right) {
                        node = parentNode;
                        rotateLeft(node);
                    }
                    node.parent.color = NodeColor.BLACK;
                    node.parent.parent.color = NodeColor.RED;
                    rotateRight(node.parent.parent);
                }
            } else {
                TreeNode<T> uncleNode = grandparentNode.left;
                
                if (uncleNode != null && uncleNode.color == NodeColor.RED) {
                    parentNode.color = NodeColor.BLACK;
                    uncleNode.color = NodeColor.BLACK;
                    grandparentNode.color = NodeColor.RED;
                    node = grandparentNode;
                } else {
                    if (node == parentNode.left) {
                        node = parentNode;
                        rotateRight(node);
                    }
                    node.parent.color = NodeColor.BLACK;
                    node.parent.parent.color = NodeColor.RED;
                    rotateLeft(node.parent.parent);
                }
            }
        }
        root.color = NodeColor.BLACK;
    }
    
    /**
     * Perform left rotation on the given node.
     */
    private void rotateLeft(TreeNode<T> node) {
        TreeNode<T> rightChild = node.right;
        
        node.right = rightChild.left;
        if (rightChild.left != null) {
            rightChild.left.parent = node;
        }
        
        rightChild.parent = node.parent;
        if (node.parent == null) {
            root = rightChild;
        } else if (node == node.parent.left) {
            node.parent.left = rightChild;
        } else {
            node.parent.right = rightChild;
        }
        
        rightChild.left = node;
        node.parent = rightChild;
    }
    
    /**
     * Perform right rotation on the given node.
     */
    private void rotateRight(TreeNode<T> node) {
        TreeNode<T> leftChild = node.left;
        
        node.left = leftChild.right;
        if (leftChild.right != null) {
            leftChild.right.parent = node;
        }
        
        leftChild.parent = node.parent;
        if (node.parent == null) {
            root = leftChild;
        } else if (node == node.parent.right) {
            node.parent.right = leftChild;
        } else {
            node.parent.left = leftChild;
        }
        
        leftChild.right = node;
        node.parent = leftChild;
    }
}
