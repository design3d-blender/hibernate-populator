# Hibernate-Populator
This project is a fork of [jpaMock](https://github.com/celiovasconcelos/jpamock) but with some added features. Functionally is about the same, so his explanation of how the project works i still the best way to describe it, please read it before you start.

It is intended for testing applications that use a large number of entities that are internally related with eachother, causing the problem of having to create very complex/long insert statment just for simple tests.


## What was added/changed?
 * Added support for hybrid JPA entity annotations, mostly for legacy apps (fields, methods or both).
 * Removed all in-app methods for assertion since it is made to work in tandem with testing frameworks now (tested with Arquillian).
 * Added support for sequences.
 * For now the "offline-mode" that makes a entity that isn't persisted is not added, yet iw would be a simple thing to add if requested.
 * Added checks for bean validations and restrictions.
 * And finally, a way for exporting and importing the created entities into a backup file for faster execution after the initial run.

## Usage
Below i will show an example of a service test using Arquillian and the Hibernate-Populator...

```java
//This will probably extend from an AbstractTest that takes care of all the testing enviroment configuration.
public class TemplateServiceTest extends AbstractTest {

	// INJECT SERVICE HERE

	@PersistenceContext
	EntityManager em;

	@Inject
	UserTransaction utx;

	private static boolean initialized = false;

	/**
	 * CHANGE THIS TO UPDATE THE BACKUP FILE
	 * 
	 * useBackup = true -> Use the backup saved to populator.sql
	 * useBackup = false -> Create or update populator.sql
	 * 
	 * filepath = "..." -> full path+filename e.g.: "C:\\Documents\\populator.sql" or "~/populator.sql"
	 */
	private static boolean useBackup = false;
	private static String filepath = "C:\\Documents\\populator.sql";

	/**
	 * In here be set this up so that we persist all data only once and the we
         * run all tests with it, this is up to preference
         *
	 * @throws Exception
	 */
	@Before
	public void preparePersistenceTest() throws Exception {
		if (!initialized) {
			clearData();
			if (useBackup) {
				insertBackupData();
			} else {
				insertData();
			}
			startTransaction();
			initialized = true;
		} else {
			startTransaction();
		}
	}

	@After
	public void afterTest() throws Exception {
		finishTransaction();
	}

	/**
	 * In here we create our entities and set them up
	 * 
	 * @throws Exception
	 */
	@Transactional
	private void insertData() throws Exception {
		utx.begin();
		em.joinTransaction();

		Populator populator = new Populator(em);

		System.out.println("Inserting records...");

		/**
		 * PERSIST TEST ENTITIES HERE
		 */
    
                //We can set our values before we persist our entity
                populator.when(Object.class, "fieldName").thenInject("someValue");
                populator.when(Object.class, "foo").thenInject(bar);
		            populator.populate(Object.class);
    
                //Or can set them after we persist our entity
                Object object = populator.populate(Object.class);
                object.setFieldName.("someValue")
                object.setFoo(bar);
    
		/**
		 * END OF PERSIST SECTION
		 */

		populator.saveFile(filepath); // save a .sql file for faster testing
		em.flush();
		utx.commit();
		// clear the persistence context (first-level cache)
		em.clear();
	}

	/**
	 * In here we persist the objects that were created previously for a "snappier experience"
         *
	 * @throws Exception
	 */
	@Transactional
	private void insertBackupData() throws Exception {
		utx.begin();
		em.joinTransaction();
    
		Populator populator = new Populator(em);

		System.out.println("Inserting records...");

		populator.populateFromFile(filepath);
    
		em.flush();
		utx.commit();
		// clear the persistence context (first-level cache)
		em.clear();
	}

	private void startTransaction() throws Exception {
		utx.begin();
		em.joinTransaction();
	}

	private void finishTransaction() throws Exception {
		utx.commit();
		em.clear();
	}

	@Test
	public void testExample() {
		List<Object> objects = em.createQuery("from Object", Object.class).getResultList();
		assertThat(objects).isNotNull();
              ...
              ..
               .
	}

}
```

Now you can add or "populate" multiple entities with the Populator and it will take care of all.

## Important remarks

For now it comes configured to work with H2 databases, by changing some small lines of code (just your specific sql command for disabling foreign keys) and it could work for most databases. That is temprary because i will probably add the way for it to work automatically in the future.

Also, if you come from [jpaMock](https://github.com/celiovasconcelos/jpamock), the main thing that changed is that the mocker a.k.a. Populator now takes in the EntityManager instead of the EntityManagerFactory on its constructor.

## How it works?

Just like [jpaMock](https://github.com/celiovasconcelos/jpamock), Populator uses JPA entity annotations to create the fake values or custom values that shall be used to create each entity (and more importantly all entities that define it). Let's take for example the Student class from [this example](https://www.baeldung.com/jpa-entities)...

You can define it by annotating it's fields.
```java
@Entity
@Table(name="STUDENT")
public class Student {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    
    @Column(name="STUDENT_NAME", length=50, nullable=false)
    private String name;
    
    @Transient
    private Integer age;
    
    // other fields, getters and setters
}
```
Or you can define it by annotating it's respective getter methods.
```java
@Entity
@Table(name="STUDENT")
public class Student {
    
    private Long id;
    
    private String name;
    
    @Transient
    private Integer age;
    
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    public Long getId(){
      return this.id
    }
    
    @Column(name="STUDENT_NAME", length=50, nullable=false)
    public String getName(){
      return this.name
    }
    
    // other fields, getters and setters
}
```

Or a combination of both if your project requires it. 
```java
@Entity
@Table(name="STUDENT")
public class Student {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    
    private String name;
    
    @Transient
    private Integer age;
    
    public Long getId(){
      return this.id
    }
    
    @Column(name="STUDENT_NAME", length=50, nullable=false)
    public String getName(){
      return this.name
    }
    
    // other fields, getters and setters
}
```

It will also do this all automatically and will take in account sequenced values and bean validations such as unique, length, etc. 

Finally it will also find and create all hidden tables and relationships between entities. All other values will be defined either by the user or by the Defaults.java class found in /populator/instance.

## Contributing
Please feel free to contribute or correct this project.

