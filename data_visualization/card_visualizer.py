
from collections import defaultdict, deque
from functools import partial
import dash
from dash import dcc
from dash import html
from dash.dependencies import Input, Output, State
import plotly.graph_objs as go
import threading
import json

from kafka_consumer import create_kafka_consumer

cards = defaultdict(partial(deque, maxlen=100))
alerts = defaultdict(partial(deque, maxlen=100))

app = dash.Dash(__name__)
app.layout = html.Div([
    dcc.Interval(id='interval-component', interval=1*1000, n_intervals=0),
    dcc.Store(id='selected-card', storage_type='session'),
    html.Div([
        html.Div([
            html.Button('Refresh Cards', id='refresh-button', n_clicks=0),
            dcc.Dropdown(id='card-dropdown', options=[], value=None),
            html.Div(id='alert-list')
        ], style={'width': '20%'}),
        html.Div([
            dcc.Graph(id='graph'),
            dcc.Graph(id='map'),
            dcc.Graph(id='time-graph'),
        ], style={'width': '80%'})
    ], style={'display': 'flex', 'height': '600px'})
])

@app.callback(
    [Output('card-dropdown', 'options'),
     Output('card-dropdown', 'value')],
    [Input('refresh-button', 'n_clicks')],
    [State('card-dropdown', 'value')]
)
def update_card_dropdown(n_clicks, selected_card):
    sorted_cards = sorted(cards.keys(), key=lambda x: len(cards[x]), reverse=True)
    if n_clicks > 0:
        card_options = [{'label': str(card_id) + " - " + str(len(cards[card_id])), 'value': card_id} for card_id in sorted_cards]
        return card_options, None
    else:
        current_options = [{'label': str(card_id) + " - " + str(len(cards[card_id])), 'value': card_id} for card_id in sorted_cards]
        return current_options, selected_card

@app.callback(
    Output('selected-card', 'data'),
    [Input('card-dropdown', 'value')]
)
def update_selected_card(selected_card):
    return selected_card

@app.callback(
    [Output('graph', 'figure'),
     Output('map', 'figure'),
     Output('time-graph', 'figure')],
    [Input('selected-card', 'data'),
     Input('interval-component', 'n_intervals')]  # Triggered by interval component
)
def update_graph(selected_card, n_intervals):
    if selected_card is None:
        return go.Figure(), go.Figure(), go.Figure()

    data = cards[selected_card]
    
    value_graph_figure = {
        'data': [go.Scatter(x=list(range(len(data))), y=[item["value"] for item in data], mode='lines+markers')],
        'layout': go.Layout(title="Transaction values")
    }
    
    map_figure = {
        'data': [
            go.Scattermapbox(
                lat=[item["latitude"] for item in data],
                lon=[item["longitude"] for item in data],
                mode='markers',
                marker=dict(size=5),
                text=[item["transaction_id"] for item in data],
            )
        ],
        'layout': go.Layout(
            mapbox=dict(
                style='open-street-map',
                zoom=3,
                center=dict(lat=data[-1]["latitude"], lon=data[-1]["longitude"])
            ),
            margin=dict(l=0, r=0, t=0, b=0)
        )
    }

    time_graph_figure = {
        'data': [go.Scatter(x=list(range(len(data))), y=[item["interval"] for item in data], mode='lines+markers')],
        'layout': go.Layout(title="Transaction intervals")
    }
    
    return value_graph_figure, map_figure, time_graph_figure

@app.callback(
    Output('alert-list', 'children'),
    [Input('selected-card', 'data'),
     Input('interval-component', 'n_intervals')]
)
def update_alerts(selected_card, n_intervals):
    if selected_card is None:
        return []

    alerts_list = [html.P(str(alert)) for alert in alerts[selected_card]]
    
    return alerts_list


def consume_kafka_messages():
    consumer = create_kafka_consumer()

    for message in consumer:
        new_data = json.loads(message.value.decode('utf-8'))
        card_num = new_data['card_num']
        interval = new_data["timestamp"] - cards[card_num][-1]["timestamp"] if card_num in cards else 0

        cards[card_num].append({
            "value": new_data["value"],
            "latitude": new_data["location"]["latitude"],
            "longitude": new_data["location"]["longitude"],
            "interval": interval,
            "timestamp": new_data["timestamp"],
            "transaction_id": new_data["transaction_id"]
        })


def consume_alerts():
    consumer = create_kafka_consumer('TransactionAlerts')

    for message in consumer:
        alert = json.loads(message.value.decode('utf-8'))
        alerts[alert['cardNumber']].append(alert)
        

if __name__ == '__main__':
    consumer_thread = threading.Thread(target=consume_kafka_messages)
    consumer_thread.daemon = True
    consumer_thread.start()

    alert_thread = threading.Thread(target=consume_alerts)
    alert_thread.daemon = True
    alert_thread.start()

    app.run_server(debug=True)
