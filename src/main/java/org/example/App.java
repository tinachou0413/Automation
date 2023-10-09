package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import org.junit.Assert;

import static java.lang.Thread.sleep;
import static java.time.Duration.ofSeconds;



public class App 
{
    public static WebDriver driver;

    public static void openWebPage (String url)
    {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        driver.get(url);
    }

    public static void enterSearchCriteria (String strSearchValue)
    {
        // There are multiple elements with "searchval" as ID (2 to be exact),
        // so need to use find_elements instead of find_element, then refer to the index.
        //  The one with index 0 is hidden and cannot have interaction (throws ElementNotInteractableException)
        List<WebElement> searchInput = driver.findElements(By.id("searchval"));
        searchInput.get(1).clear();
        searchInput.get(1).sendKeys(strSearchValue);
        searchInput.get(1).sendKeys(Keys.ENTER);
    }

    public static boolean checkExistsById (String strId) {
        // returns True if strId exists on page
        // otherwise, return False

        try {
            driver.findElement (By.id(strId));
        }
        catch (Exception e) {
            return false;
        }

        return true;
    }

    public static boolean verifySearchResultOnePage (int iPageNum, String strVerifyText) {

        int iItemsPassed = 0;

        WebDriverWait wait = new WebDriverWait(driver, ofSeconds(10));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("product_listing")));

        List<WebElement> productListingItems = driver.findElements(By.id("ProductBoxContainer"));

        int iIndex = 0;
        int iMaxIndex = productListingItems.size();

        while (iIndex < iMaxIndex) {
            WebElement curItem = productListingItems.get(iIndex);
            WebElement curItemDescription = curItem.findElement(By.xpath("//*[@data-testid=\"itemDescription\"]"));
            String strItemDescriptionText = curItemDescription.getText().toLowerCase();

            if (strItemDescriptionText.contains(strVerifyText)) {
                iItemsPassed = iItemsPassed + 1;
                Assert.assertTrue(strItemDescriptionText.contains(strVerifyText));
            }
            else
            {
                System.out.println ("Verify Search Result FAILED on page " +  iPageNum + ", Item # " + (iIndex + 1) + ".");
                Assert.assertFalse(strItemDescriptionText.contains(strVerifyText));
                return false;
            }

            iIndex = iIndex + 1;
        }

        System.out.println("Verify Search Result PASSED on page " + iPageNum + ".  " + iItemsPassed + " item(s) checked.");

        return true;
    }

    public static boolean verifySearchResult (String strVerifyText)
    {
        // Check whether there's pagination on the search results page
        boolean bPaginationBarExist = checkExistsById ("paging");

        if (!bPaginationBarExist) {

             // Search returned only 1 page of results
            System.out.println ("Search returned only 1 page of results");
            return verifySearchResultOnePage (1, strVerifyText);

        }
        else
        {
            System.out.println("Search returned multiple pages of results, need to check each page");

            int iPageNum = 1;

            // identify the page number of the last page
            WebElement lastPageLink = driver.findElement(By.xpath("//a[contains(@aria-label, 'last page')]"));
            int iLastPageNum = Integer.parseInt(lastPageLink.getText());

            // infinite loop.... until exit condition is reached within the loop itself
            while (true)
            {
                boolean bDoesPagePassedTest = verifySearchResultOnePage (iPageNum, strVerifyText);

                if (!bDoesPagePassedTest)
                {
                    return false;
                }

                // exit the "infinite" loop if code execution reaches the last page
                if (iPageNum == iLastPageNum) {
                    break;
                }

                List<WebElement> goToPageItems = driver.findElements(By.xpath("//a[contains(@aria-label, 'go to page')]"));
                WebElement nextPageLink = goToPageItems.get(goToPageItems.size()-1);

                iPageNum = Integer.parseInt(nextPageLink.getAccessibleName().split ("go to page ")[1]);
                nextPageLink.click();
            }

            return true;
        }
    }


    public static void addLastItemToCart () {

        // Add the last item from search result to cart, taking into consideration
        // there may be multiple pages of search results returned

        // Check whether there's pagination on the search results page
        boolean bPaginationBarExist = checkExistsById ("paging");

        if (!bPaginationBarExist) {

            // Search returned only 1 page of results, no action needed
            System.out.println("Search returned only 1 page of results");
        }
        else
        {

            // Search returned multiple pages of results, navigate to the last page
            System.out.println("Search returned multiple pages of results, navigate to the last page");

            WebElement lastPageLink = driver.findElement(By.xpath("//a[contains(@aria-label, \"last page\")]"));
            lastPageLink.click();
        }

        // Find the last "Add to Cart" button on page, click on it to add to cart
        List<WebElement> addToCartButtonItems = driver.findElements(By.xpath("//*[@data-testid=\"itemAddCart\"]"));
        int iMaxIndex = addToCartButtonItems.size();
        addToCartButtonItems.get(iMaxIndex-1).click();
        System.out.println("Added last item in search result to cart");

        Actions action = new Actions(driver);
        action.sendKeys(Keys.ESCAPE).perform();
    }

    public static void emptyCart() {

        //sleep for 3 seconds to allow items to be properly added to cart
        try {
            sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        WebDriverWait wait = new WebDriverWait(driver, ofSeconds(10));

        // view shopping cart
        WebElement cartButton = driver.findElement(By.id("cartItemCountSpan"));
        cartButton.click();

        // empty shopping cart
        wait.until(ExpectedConditions.elementToBeClickable(By.className("emptyCartButton")));
        WebElement emptyCartButton = driver.findElement(By.className("emptyCartButton"));
        emptyCartButton.click();

        // confirm empty shopping cart
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@role=\"alertdialog\"]//div")));
        WebElement modalContainer = driver.findElement(By.xpath("//div[@role=\"alertdialog\"]//div"));
        List<WebElement> modalAllButtons = modalContainer.findElements(By.tagName("button"));
        modalAllButtons.get(1).click();

        System.out.println("Shopping cart emptied");

    }

    public static void main (String[] args)
    {
        String url = "https://www.webstaurantstore.com/";
        String searchCriteria = "stainless work table";
        //String searchCriteria = "galvanized undershelf regency";
        //String searchCriteria = "beer soda case stacker";
        String verifySearchResultText = "table";

        openWebPage(url);

        enterSearchCriteria (searchCriteria);
        boolean bTestResult_VerifySearchResult = verifySearchResult (verifySearchResultText);

        if (bTestResult_VerifySearchResult) {
            System.out.println("Test Passed: All search result contain the word '" + verifySearchResultText + "'");
        }
        else
        {
            System.out.println ("Test Failed: One or more search result does not contain the word '" + verifySearchResultText + "'");
        }

        enterSearchCriteria (searchCriteria);
        addLastItemToCart();
        emptyCart();

        System.out.println("Done!");

        driver.close();

    }
}
