/*
 * Copyright 2011 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import com.splunk.Entity;
import com.splunk.EntityCollection;

import java.lang.reflect.Constructor;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

// Abstract class that generalizes an explorer node for any EntityCollection
class EntityCollectionNode extends ExplorerNode {
    Class itemClass;
    Constructor itemCtor = null;

    EntityCollectionNode(String title, EntityCollection value, Class itemClass) 
    {
        super(value, new NoKids());
        this.itemClass = itemClass;
        setDisplayName(String.format("%s (%d)", title, value.size()));
        setChildren(new EntityCollectionKids(this));
    }

    Node createKid(Entity entity) {
        try {
            if (itemCtor == null)
                itemCtor = itemClass.getDeclaredConstructor(Entity.class);
            return (Node)itemCtor.newInstance(entity);
        }
        catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    EntityCollection getCollection() {
        return (EntityCollection)this.value;
    }

    @Override PropertyList getMetadata() {
         return new PropertyList() {{
            add(int.class, "size");
        }};
    }

    class EntityCollectionKids extends Children.Keys<Entity> {
        EntityCollectionNode parent;

        EntityCollectionKids(EntityCollectionNode parent) {
            this.parent = parent;
        }

        @Override protected void addNotify() {
            setKeys(parent.getCollection().values());
        }

        @Override protected Node[] createNodes(Entity entity) {
            return new Node[] { parent.createKid(entity) };
        }
    }
}
