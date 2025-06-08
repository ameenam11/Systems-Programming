#include "../include/Order.h"
#include"../include/Customer.h"
#include"../include/WareHouse.h"
#include <iostream>
#include <string>
#include <vector>
using std::string;
using std::vector;


 
    
        Customer::Customer(int id, const string &name, int locationDistance, int maxOrders) 
        : id(id), name(name), locationDistance(locationDistance), maxOrders(maxOrders), ordersId()
        {}
        const string& Customer::getName() const
        {
            return name;
        }
        int Customer::getId() const
        {
            return id;
        }
        int Customer::getCustomerDistance() const
        {
            return locationDistance;
        }
        int Customer::getMaxOrders() const //Returns maxOrders
        {
            return maxOrders;
        }
        int Customer::getNumOrders() const //Returns num of orders the customer has made so far
        {
            return ordersId.size();
        }
        bool Customer::canMakeOrder() const //Returns true if the customer didn't reach max orders
        {
            int size = ordersId.size();
            return size < maxOrders;
        }
        const vector<int>& Customer::getOrdersIds() const
        {
            return ordersId;
        }
        int Customer::addOrder(int orderId) //return OrderId if order was added successfully, -1 otherwise
        {
            if(canMakeOrder())
            {
                this->ordersId.push_back(orderId);
                return orderId;
            }
            return -1;
        }

        

        
  


 
    
        SoldierCustomer::SoldierCustomer(int id, const string &name, int locationDistance, int maxOrders)
        : Customer(id, name, locationDistance, maxOrders)
        {}  
        SoldierCustomer* SoldierCustomer::clone() const 
        {
            return new SoldierCustomer(*this);
        }
    
    
        



    
        CivilianCustomer::CivilianCustomer(int id, const string &name, int locationDistance, int maxOrders)
        : Customer(id, name, locationDistance, maxOrders)
        {}  
        CivilianCustomer* CivilianCustomer::clone() const 
        {
            return new CivilianCustomer(*this);
        }
    
    
        
