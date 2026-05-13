public class ProxyPatternLazyLoadingDemo {

    // SUBJECT
    interface Image {
        void display();
    }

    // REAL OBJECT
    static class RealImage implements Image {

        private String fileName;

        public RealImage(String fileName) {
            this.fileName = fileName;
            loadFromDisk();
        }

        private void loadFromDisk() {
            System.out.println("Loading high resolution image from disk: " + fileName);
        }

        @Override
        public void display() {
            System.out.println("Displaying image: " + fileName);
        }
    }

    // PROXY
    static class ProxyImage implements Image {

        private String fileName;
        private RealImage realImage;

        public ProxyImage(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void display() {

            // Lazy Loading
            if (realImage == null) {
                realImage = new RealImage(fileName);
            }

            realImage.display();
        }
    }

    // CLIENT
    public static void main(String[] args) {
        Image image = new ProxyImage("vacation_photo.png");
        System.out.println("Image object created");
        System.out.println();

        // Actual image not loaded yet

        System.out.println("User clicks image...");
        image.display();

        System.out.println();

        // Already loaded, no reload happens
        System.out.println("User opens image again...");
        image.display();
    }
}
