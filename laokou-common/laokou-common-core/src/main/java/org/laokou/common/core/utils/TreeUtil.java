/**
 * Copyright (c) 2022 KCloud-Platform-Alibaba Authors. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *   http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.laokou.common.core.utils;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.laokou.common.i18n.core.CustomException;

import java.util.*;

/**
 * @author laokou
 */
@Data
public class TreeUtil {
    public static <T> TreeNode<T> rootRootNode(String name) {
        return new TreeNode<>(0L,name,null, new ArrayList<>(0));
    }
    public static <T> TreeNode<T> rootRootNode() {
        return rootRootNode("根节点");
    }
    public static <T extends TreeNode<T>> T buildTreeNode(List<T> treeNodes,T rootNode) {
        if (null == rootNode) {
            throw new CustomException("请构造根节点");
        }
        treeNodes.add(rootNode);
        // list转map
        Map<Long, T> nodeMap = new LinkedHashMap<>(treeNodes.size());
        for (T treeNode : treeNodes) {
            nodeMap.put(treeNode.getId(), treeNode);
        }
        for (T treeNo : treeNodes) {
            T parent = nodeMap.get(treeNo.getPid());
            if (parent != null && treeNo.getPid().equals(parent.getId())) {
                if (CollectionUtils.isEmpty(parent.getChildren())) {
                    parent.setChildren(new ArrayList<>(0));
                }
                parent.getChildren().add(treeNo);
            }
        }
        return rootNode;
    }
    public static class TreeNode<T> {
        private Long id;
        private String name;
        private Long pid;
        private List<T> children;
        public TreeNode() {}
        public TreeNode(Long id, String name, Long pid, List<T> children) {
            this.id = id;
            this.name = name;
            this.pid = pid;
            this.children = children;
        }
        @Override
        public String toString() {
            return new StringJoiner(", ", TreeNode.class.getSimpleName() + "[", "]")
                    .add("id='" + id + "'")
                    .add("name='" + name + "'")
                    .add("pid='" + pid + "'")
                    .add("children=" + children)
                    .toString();
        }
        public Long getId() {
            return id;
        }
        public void setId(Long id) {
            this.id = id;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public Long getPid() {
            return pid;
        }
        public void setPid(Long pid) {
            this.pid = pid;
        }
        public List<T> getChildren() {
            return children;
        }
        public void setChildren(List<T> children) {
            this.children = children;
        }
    }
}
