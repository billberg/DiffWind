package sort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @date: 2015年6月19日 上午10:11:44
 */

public class CollectionTest {
    /**
     * 主方法
     * 
     * @param args
     *            参数
     * @modify by user: {修改人} 2015年6月19日
     * @modify by reason:{原因}
     */
    public static void main(String[] args) {
        List<CollectionTest.Man> mans = new ArrayList<CollectionTest.Man>();
        CollectionTest collectionTest = new CollectionTest();
        Man man = collectionTest.new Man();
        man.setParentId(11);
        man.setAge(12);
        man.setName("某某1_孩子");
        Man man2 = collectionTest.new Man();
        man2.setParentId(2);
        man2.setAge(13);
        man2.setName("某某2_孩子");
        Man man3 = collectionTest.new Man();
        man3.setParentId(2);
        man3.setAge(16);
        man3.setName("某某1_孩子");
        mans.add(man);
        mans.add(man2);
        mans.add(man3);
        sortAge(mans);
        System.out.println("*****根据年龄排序********");
        System.out.println(mans.toString());
        sortParentIdAge(mans);
        System.out.println("*****根据父ID，年龄排序********");
        System.out.println(mans.toString());
    }

    /**
     * 根据父ID，年龄进行排序
     * 
     * @param mans
     *            人列表
     * @modify by user: {修改人} 2015年6月19日
     * @modify by reason:{原因}
     */
    private static void sortParentIdAge(List<Man> mans) {
        Collections.sort(mans, new Comparator<Man>() {
            @Override
            public int compare(Man o1, Man o2) {
                if (o1.getParentId().equals(o2.getParentId())) {
                    return o1.getAge().compareTo(o2.getAge());
                } else {
                    return o1.getParentId().compareTo(o2.getParentId());
                }
            }
        });
    }

    /**
     * 根据父ID排序
     * 
     * @param mans
     * @modify by user: {修改人} 2015年6月19日
     * @modify by reason:{原因}
     */
    private static void sortAge(List<Man> mans) {
        Collections.sort(mans, new Comparator<Man>() {
            @Override
            public int compare(Man o1, Man o2) {
                return o1.getAge().compareTo(o2.getAge());
            }
        });
    }

    public class Man {
        
        private Integer parentId;

        private Integer age;

        private String name;

        /**
         * 获取parentId
         * 
         * @return parentId parentId
         */
        public Integer getParentId() {
            return parentId;

        }

        /**
         * 设置parentId
         * 
         * @param parentId
         *            parentId
         */
        public void setParentId(Integer parentId) {
            this.parentId = parentId;
        }

        /**
         * 获取age
         * 
         * @return age age
         */
        public Integer getAge() {
            return age;

        }

        /**
         * 设置age
         * 
         * @param age
         *            age
         */
        public void setAge(Integer age) {
            this.age = age;
        }

        /**
         * 获取name
         * 
         * @return name name
         */
        public String getName() {
            return name;

        }

        /**
         * 设置name
         * 
         * @param name
         *            name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return
         * @modify by user: {修改人} 2015年6月19日
         * @modify by reason:{原因}
         */
        @Override
        public String toString() {
            return "Man [parentId=" + parentId + ", age=" + age + ", name=" + name + "]";
        }

    }
}
