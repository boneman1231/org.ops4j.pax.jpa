/*
 * Copyright 2012 Harald Wellmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.jpa.impl;

import java.lang.instrument.IllegalClassFormatException;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.spi.ClassTransformer;

import org.ops4j.pax.jpa.impl.descriptor.PersistenceUnitInfoImpl;
import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JpaWeavingHook implements WeavingHook
{
    private static final Logger LOG = LoggerFactory.getLogger( JpaWeavingHook.class ); 
    
    private PersistenceUnitInfoImpl puInfo;
    private ClassTransformer transformer;
    private BundleClassLoader cl;
    private Set<String> managedClasses;

    public JpaWeavingHook( PersistenceUnitInfoImpl puInfo, ClassTransformer transformer )
    {
        this.puInfo = puInfo;
        this.transformer = transformer;
        this.cl = new BundleClassLoader( puInfo.getBundle() );
        this.managedClasses = new HashSet<String>( puInfo.getManagedClassNames() );
    }

    @Override
    public void weave( WovenClass wovenClass )
    {
        if( wovenClass.getBundleWiring().getBundle() == puInfo.getBundle()
                && managedClasses.contains( wovenClass.getClassName() ) )
        {
            try
            {
                LOG.info ("weaving {}", wovenClass.getClassName());
                byte[] transformed =
                    transformer.transform( cl, wovenClass.getClassName(),
                        wovenClass.getDefinedClass(), wovenClass.getProtectionDomain(),
                        wovenClass.getBytes() );
                wovenClass.setBytes( transformed );
                wovenClass.getDynamicImports().add( "org.apache.openjpa.enhance" );
                wovenClass.getDynamicImports().add( "org.apache.openjpa.util" );
                wovenClass.getDynamicImports().add( "org.eclipse.persistence.*" );
            }
            catch ( IllegalClassFormatException exc )
            {
                throw new WeavingException( "cannot transform " + wovenClass.getClassName(), exc );
            }
        }
    }

}
