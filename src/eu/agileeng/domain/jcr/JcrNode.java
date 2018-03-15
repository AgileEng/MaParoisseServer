package eu.agileeng.domain.jcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEObject;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEDynamicProperties;
import eu.agileeng.util.AEStringUtil;

public abstract class JcrNode extends AEDomainObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = -743625295087589311L;
	
	static public final class JSONKey {
		public static final String ownerId = "ownerId";
		public static final String system = "system";
		public static final String moduleId = "moduleId";
		public static final String path = "path";
		public static final String destPath = "destPath";
		public static final String newName = "newName";
		public static final String jcr_path = "jcr:path";
		public static final String newNodeName = "newNodeName";
		public static final String mimeType = "mimeType";
		public static final String primaryNodeType = "primaryNodeType";
		public static final String jcr_primaryType = "jcr:primaryType";
		public static final String jcrContentFileName = "jcrContentFileName";
		public static final String node = "node";
		public static final String folders = "folders";
		public static final String content = "content";
		public static final String fullTextSearchExpression = "fullTextSearchExpression";
		public static final String searchResult = "searchResult";
		public static final String versionHistory = "versionHistory";
		public static final String versionName = "versionName";
		public static final String versionPath = "versionPath";
		public static final String note = "note";
		public static final String password = "password";
		
		public static final String ae_displayName = "ae:displayName";
		public static final String ae_lastModified = "ae:lastModified";
		
		public static final String id = "id";
		public static final String text = "text";
		public static final String leaf = "leaf";
		public static final String dontValidate = "dontValidate";
	}
	
	static public class JcrProperty {
		public static final String created = "jcr:created";
		public static final String createdBy = "jcr:createdBy";
		public static final String lastModified = "jcr:lastModified";
		public static final String lastModifiedBy = "jcr:lastModifiedBy";
		public static final String primaryType = "jcr:primaryType";
		public static final String mixinTypes = "jcr:mixinTypes";
		public static final String protocol = "jcr:protocol";
		public static final String host = "jcr:host";
		public static final String port = "jcr:port";
		public static final String repository = "jcr:repository";
		public static final String workspace = "jcr:workspace";
		public static final String path = "jcr:path";
		public static final String id = "jcr:id";
		public static final String address = "jcr:address";
		public static final String children = "jcr:children";
		public static final String mimeType = "jcr:mimeType";
		public static final String encoding = "jcr:encoding";
		public static final String content = "jcr:content";
		public static final String data = "jcr:data";
		public static final String title = "jcr:title";
		public static final String description = "jcr:description";
		public static final String rootVersion = "jcr:rootVersion";
		
		public static final String ae_created = "ae:created";
		public static final String ae_createdBy = "ae:createdBy";
		public static final String ae_createdByRole = "ae:createdByRole";
		public static final String ae_lastModified = "ae:lastModified";
		public static final String ae_lastModifiedBy = "ae:lastModifiedBy";
		public static final String ae_size = "ae:size";
		public static final String ae_fileExtension = "ae:fileExtension";
		public static final String ae_iconClass = "ae:iconClass";
		public static final String ae_system = "ae:system";
		public static final String ae_code = "ae:code";
		public static final String ae_displayName = "ae:displayName";
		public static final String ae_baseVersion = "ae:baseVersion";
	}
	
	static public class JcrNodeType {
		public static final String unstructured = "nt:unstructured";
		public static final String resource = "nt:resource";
		public static final String file = "nt:file";
		public static final String folder = "nt:folder";
		public static final String version = "nt:version";
		public static final String mixUnstructured = "mix:unstructured";
		public static final String mixLockable = "mix:lockable";
		public static final String mixTitle = "mix:title";
	}
	
	static public class JcrModule {
		public static final String Comptabilite = "Comptabilité";
		public static final String Social = "Social";
	}
	
	public String displayName;
	
	public String path;
	
	public String primaryNodeType;

	public AEDynamicProperties jcrProperties = new AEDynamicProperties();

	public JcrNode() {
		super(DomainModel.DomainClass.JcrNode);
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPrimaryNodeType() {
		return primaryNodeType;
	}

	public void setPrimaryNodeType(String primaryNodeType) {
		this.primaryNodeType = primaryNodeType;
	}

	public AEDynamicProperties getJcrProperties() {
		return jcrProperties;
	}

	public void setJcrProperties(AEDynamicProperties jcrProperties) {
		this.jcrProperties = jcrProperties;
	}
	
	protected void create(Node node) throws AEException {
		try {
			setID(AEPersistentUtil.getTmpID());
			setName(node.getName());
			setPath(node.getPath());
			setPrimaryNodeType(node.getPrimaryNodeType().getName());
			
			// JcrNode.JcrProperty.ae_displayName
			try {
				setDisplayName(getName());
				javax.jcr.Property displayNameProperty = node.getProperty(JcrNode.JcrProperty.ae_displayName);
				if(displayNameProperty != null) {
					setDisplayName(displayNameProperty.getValue().getString());
				}
			} catch (Exception e) {}
			
			// JcrNode.JcrProperty.ae_system
			try {
				javax.jcr.Property isSystemProperty = node.getProperty(JcrNode.JcrProperty.ae_system);
				if(isSystemProperty != null) {
					boolean isSystem = isSystemProperty.getValue().getBoolean();
					if(isSystem) {
						setProperties(AEObject.Property.SYSTEM);
						setDescription("Système");
					} else {
						removeProperty(AEObject.Property.SYSTEM);
					}
				}
			} catch (Exception e) {}
			
			// JcrNode.JcrProperty.ae_code
			try {
				javax.jcr.Property codeProperty = node.getProperty(JcrNode.JcrProperty.ae_code);
				if(codeProperty != null) {
					setCode(codeProperty.getValue().getString());
				}
			} catch (Exception e) {}
			
			// properties
			javax.jcr.PropertyIterator propertyIterator = node.getProperties();
			for (int j = 0; j < propertyIterator.getSize(); j++) {
				javax.jcr.Property property = propertyIterator.nextProperty();
				if(!property.isMultiple()) {
					if (JcrNode.JcrProperty.data.equalsIgnoreCase(property.getName())) {
						//added a Binary Value text to the jcr:data property, 
						//as displaying raw binary in the value cell isn't desirable
						jcrProperties.put(property.getName() + " (Click to open)", "Binary Value");
					} else if (null != property.getValue() && property.getValue().getString().startsWith("http://")) {
						//Any property of value which starts with http:// will be openable in the frontend 
						jcrProperties.put(property.getName() + " (Click to open)", property.getValue().getString());
					} else if(!AEStringUtil.isEmpty(property.getName()) && property.getValue() != null) {
						// keep the property
						jcrProperties.put(property.getName(), property.getValue().getString());
					}
				} else {
					Value[] valuesArray = property.getValues();
					Collection<String> values = new ArrayList<String>();
					for (int i = 0; i < valuesArray.length; i++) {
						values.add(valuesArray[i].getString());
					}
					jcrProperties.put(property.getName(), values);
				}
			}
			
			// custom properties
			if(JcrNode.isVersionControlled(node)) {
				VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
				try {
					Version baseVersion = versionManager.getBaseVersion(node.getPath());
					if(!JcrNode.JcrProperty.rootVersion.equals(baseVersion.getName())) {
						jcrProperties.put(JcrNode.JcrProperty.ae_baseVersion, baseVersion.getName());
					}
				} catch(Exception e) {}
			}
		} catch (RepositoryException e) {
			throw new AEException(e);
		}
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		if(hasProperty(AEObject.Property.SYSTEM)) {
			json.put(JcrNode.JSONKey.system, true);
		}
		
		json.put(JcrNode.JSONKey.jcr_path, getPath());
		json.put(JcrNode.JSONKey.id, getPath());
		json.put(JcrNode.JSONKey.text, getDisplayName());
		json.put(JcrNode.JSONKey.ae_displayName, getDisplayName());
		json.put(JcrNode.JSONKey.leaf, false);
		
		if(jcrProperties != null) {
			@SuppressWarnings("unchecked")
			Set<String> jcrKeySet = jcrProperties.keySet();
			for (String property : jcrKeySet) {
				json.put(property, jcrProperties.get(property));
			}
		}
		
		return json;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	/**
     * @return true, if specified <code>node</code> has the mixin nodetype 'mix:versionable' set.
     */
    public static boolean isVersionControlled(Node node) {
        boolean vc = false;
        if (node != null) {
            try {
                vc = node.isNodeType(NodeType.MIX_VERSIONABLE) || node.isNodeType(NodeType.MIX_SIMPLE_VERSIONABLE);
            } catch (RepositoryException e) {
                AEApp.logger().warn(e.getMessage());
            }
        }
        return vc;
    }
}
