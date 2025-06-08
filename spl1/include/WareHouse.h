#pragma once
#include <string>
#include <vector>

#include "Order.h"
#include "Customer.h"

class BaseAction;
class Volunteer;

// Warehouse responsible for Volunteers, Customers Actions, and Orders.


class WareHouse {

    public:
        WareHouse& operator=(WareHouse&& other) noexcept;
        WareHouse& operator=(const WareHouse& other);
        WareHouse(WareHouse&& other) noexcept;
        ~WareHouse();
        WareHouse(const WareHouse& WareHouse);

        WareHouse(const string &configFilePath);
        void start();
        void addOrder(Order* order);
        void addAction(BaseAction* action);
        void addCustomer(Customer* customer);
        Customer &getCustomer(int customerId) const;
        Volunteer &getVolunteer(int volunteerId) const;
        Order &getOrder(int orderId) const;
        const vector<BaseAction*> &getActions() const;
        const vector<Order*>& getPendingOrders() const;
        const vector<Order*>& getInProcessOrders() const;
        const vector<Order*>& getCompletedOrders() const;

        void close();
        void open();
        int getCustomerCounter() const;
        int getOrderCounter() const;
        int getVolunteerCounter() const;
        void step();
        void sortVector(std::vector<int>& vec);
        void backUpWareHouse();
        void restoreWareHouse();
        bool getIsBackedUp() const;

    private:
        bool isOpen;
        bool isBackedUp;
        vector<BaseAction*> actionsLog;
        vector<Volunteer*> volunteers;
        vector<Order*> pendingOrders;
        vector<Order*> inProcessOrders;
        vector<Order*> completedOrders;
        vector<Customer*> customers;
        int customerCounter; //For assigning unique customer IDs
        int volunteerCounter; //For assigning unique volunteer IDs
        int orderCounter;//For assigning unique order IDs
};